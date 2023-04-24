/*
 * Copyright © 2023 Tobias Gierke (tobias.gierke@code-sourcery.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.maven.buildprofiler.server.db;

import de.codesourcery.maven.buildprofiler.common.Interval;
import de.codesourcery.maven.buildprofiler.server.model.Artifact;
import de.codesourcery.maven.buildprofiler.server.model.ArtifactId;
import de.codesourcery.maven.buildprofiler.server.model.Build;
import de.codesourcery.maven.buildprofiler.server.model.Host;
import de.codesourcery.maven.buildprofiler.server.model.LifecyclePhase;
import de.codesourcery.maven.buildprofiler.server.model.Record;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wicket.util.string.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Repository
public class DAO
{
    private static final Logger LOG = LogManager.getLogger( DAO.class );

    // tables
    private static final String BUILDS_TABLE = "profiler.builds";
    private static final String PHASES_TABLE = "profiler.phases";
    private static final String ARTIFACTS_TABLE = "profiler.artifacts";
    private static final String RECORDS_TABLE = "profiler.records";
    private static final String HOSTS_TABLE = "profiler.hosts";

    // cols
    private static final String BUILD_ID_COL = "build_id";
    private static final String BUILD_START_TIME_COL = "build_start_time";
    private static final String BUILD_DURATION_COL = "build_duration_millis";
    private static final String BUILD_HOST_ID_COL = "host_id";
    private static final String BUILD_PROJECT_NAME_COL = "project_name";
    private static final String BUILD_BRANCH_NAME_COL = "branch_name";
    private static final String BUILD_MAX_CONCURRENCY_COL = "max_concurrency";
    private static final String BUILD_JVM_VERSION_COL = "jvm_version";
    private static final String BUILD_AVAILABLE_CPUS_COL = "available_processors";
    private static final String BUILD_GIT_HASH_COL = "git_hash";
    private static final String BUILD_SYSTEM_PROPERTIES_COL = "system_properties";
    private static final String BUILD_ENV_PROPERTIES_COL = "env_properties";

    private static final List<String> BUILD_TABLE_COLS = List.of(
        BUILD_ID_COL,
        BUILD_START_TIME_COL,
        BUILD_DURATION_COL,
        BUILD_HOST_ID_COL,
        BUILD_PROJECT_NAME_COL,
        BUILD_BRANCH_NAME_COL,
        BUILD_MAX_CONCURRENCY_COL,
        BUILD_JVM_VERSION_COL,
        BUILD_AVAILABLE_CPUS_COL,
        BUILD_GIT_HASH_COL,
        BUILD_SYSTEM_PROPERTIES_COL,
        BUILD_ENV_PROPERTIES_COL );

    private static final String HOST_ID_COL = "host_id";
    private static final String HOST_NAME_COL = "host_name";
    private static final String HOST_IP_COL = "host_ip";

    private static final List<String> HOST_TABLE_COLS = List.of( HOST_ID_COL, HOST_NAME_COL, HOST_IP_COL );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static final class SearchCriteria implements Serializable
    {
        public enum SortColumn
        {
            BUILD_TIMESTAMP,
            PROJECT_NAME,
            BRANCH_NAME,
            HOST,
            BUILD_DURATION
        }

        public String projectName;
        public String branchName;
        public Host host;
        public Interval interval;
        public List<LifecyclePhase> phases = new ArrayList<>();
        public int offset;
        public int limit;
        public SortColumn sortColumn;
        public boolean sortAscending;

        public SearchCriteria()
        {
        }

        public SearchCriteria(SearchCriteria other)
        {
            this.projectName = other.projectName;
            this.branchName = other.branchName;
            ;
            this.host = other.host;
            this.interval = other.interval;
            this.phases = other.phases;
            this.offset = other.offset;
            this.limit = other.limit;
            this.sortColumn = other.sortColumn;
            this.sortAscending = other.sortAscending;
        }

        public SearchCriteria withHost(Host host)
        {
            final SearchCriteria result = new SearchCriteria( this );
            result.host = host;
            return result;
        }

        public SearchCriteria withBranchName(String branchName)
        {
            final SearchCriteria result = new SearchCriteria( this );
            result.branchName = branchName;
            return result;
        }

        public SearchCriteria forCountQuery()
        {
            final SearchCriteria result = new SearchCriteria( this );
            result.sortColumn = null;
            result.limit = 0;
            return result;
        }
    }

    private static final class BuildRowMapper implements RowMapperHelper<Build>
    {
        private final org.springframework.jdbc.core.RowMapper<Host> hostsMapper;
        private final String idCol;
        private final String startTimeCol;
        private final String durationCol;
        private final String hostIdCol;
        private final String projectNameCol;
        private final String branchNameCol;
        private final String jvmVersionCol;
        private final String availableCpusCol;
        private final String gitHashCol;
        private final String systemPropertiesCol;
        private final String envPropertiesCol;

        public BuildRowMapper(String buildsTableColPrefix, String hostsTableColPrefix)
        {
            hostsMapper = new HostsMapper( hostsTableColPrefix, true );
            idCol = buildsTableColPrefix + BUILD_ID_COL;
            startTimeCol = buildsTableColPrefix + BUILD_START_TIME_COL;
            durationCol = buildsTableColPrefix + BUILD_DURATION_COL;
            hostIdCol = buildsTableColPrefix + BUILD_HOST_ID_COL;
            projectNameCol = buildsTableColPrefix + BUILD_PROJECT_NAME_COL;
            branchNameCol = buildsTableColPrefix + BUILD_BRANCH_NAME_COL;
            jvmVersionCol = buildsTableColPrefix + BUILD_JVM_VERSION_COL;
            availableCpusCol = buildsTableColPrefix + BUILD_AVAILABLE_CPUS_COL;
            gitHashCol = buildsTableColPrefix + BUILD_GIT_HASH_COL;
            systemPropertiesCol = buildsTableColPrefix + BUILD_SYSTEM_PROPERTIES_COL;
            envPropertiesCol = buildsTableColPrefix + BUILD_ENV_PROPERTIES_COL;
        }

        @Override
        public Build mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            final Build result = new Build();
            result.id = rs.getLong( idCol );
            result.startTime = dateTime( startTimeCol, rs );
            result.duration = duration( durationCol, ChronoUnit.MILLIS, rs );
            result.host = hostsMapper.mapRow( rs, rowNum );
            result.projectName = rs.getString( projectNameCol );
            result.branchName = rs.getString( branchNameCol );
            result.jvmVersion = rs.getString( jvmVersionCol );
            result.availableProcessors = rs.getInt( availableCpusCol );
            result.gitHash = rs.getString( gitHashCol );
            result.systemProperties = rs.getString( systemPropertiesCol );
            result.environmentProperties = rs.getString( envPropertiesCol );
            return result;
        }
    }

    private static final class ArtifactMapper implements RowMapper<Artifact>
    {

        @Override
        public Artifact mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            final Artifact r = new Artifact();
            r.id = rs.getLong( "artifact_id" );
            r.groupId = rs.getString( "group_id_txt" );
            r.artifactId = rs.getString( "artifact_id_txt" );
            return r;
        }
    }

    private static final class HostsMapper implements RowMapper<Host>
    {
        private final Map<Long, Host> cache;
        private final String hostIdCol;
        private final String hostNameCol;
        private final String hostIPCol;

        public HostsMapper()
        {
            this( "", false );
        }

        public HostsMapper(boolean cacheInstances)
        {
            this( "", cacheInstances );
        }

        public HostsMapper(String hostsTableColPrefix, boolean cacheInstances)
        {
            this.cache = cacheInstances ? new HashMap<>() : null;
            hostIdCol = hostsTableColPrefix + HOST_ID_COL;
            hostNameCol = hostsTableColPrefix + HOST_NAME_COL;
            hostIPCol = hostsTableColPrefix + HOST_IP_COL;
        }

        @Override
        public Host mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            final long hostID = rs.getLong( hostIdCol );

            Host h;
            if ( cache != null )
            {
                h = cache.get( hostID );
                if ( h != null )
                {
                    return h;
                }
            }
            h = new Host();
            h.hostId = hostID;
            h.setHostName( rs.getString( hostNameCol ) );
            final String ip = rs.getString( hostIPCol );
            try
            {
                h.setHostIP( InetAddress.getByName( ip ) );
            }
            catch( UnknownHostException e )
            {
                throw new SQLException( "Failed to convert '" + ip + "' to an InetAddress", e );
            }
            if ( cache != null )
            {
                cache.put( hostID, h );
            }
            return h;
        }
    }

    private static final class RecordMapper implements RowMapperHelper<Record>
    {
        @Override
        public Record mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            final Record result = new Record();
            result.id = rs.getLong( "record_id" );
            result.buildId = rs.getLong( "build_id" );
            result.phaseId = rs.getLong( "phase_id" );
            result.pluginArtifactId = rs.getLong( "plugin_artifact_id" );
            result.pluginVersion = rs.getString( "plugin_version" );
            result.artifactId = rs.getLong( "artifact_id" );
            result.artifactVersion = rs.getString( "artifact_version" );
            result.startTime = dateTime( "start_time", rs );
            result.endTime = dateTime( "end_time", rs );
            return result;
        }
    }

    public int getBuildCount(SearchCriteria crit)
    {
        // make sure we're not generating unnecessary ORDER BY
        // or harmful LIMIT/OFFSET clauses
        final SearchCriteria forCount = crit.forCountQuery();
        return jdbcTemplate.execute( (ConnectionCallback<Integer>) con -> {

            try ( final PreparedStatement stmt = createPreparedQueryClause( "SELECT COUNT(DISTINCT build_id) FROM " + BUILDS_TABLE, forCount, con ) )
            {
                final int count;
                try ( final ResultSet rs = stmt.executeQuery() )
                {
                    if ( !rs.next() )
                    {
                        throw new EmptyResultDataAccessException( "Empty result set?", 1 );
                    }
                    count = rs.getInt( 1 );
                }
                return count;
            }
        } );
    }

    public List<Build> getBuild(SearchCriteria criteria)
    {
        return jdbcTemplate.execute( (ConnectionCallback<List<Build>>) con ->
        {
            final List<String> allColumns = BUILD_TABLE_COLS.stream().map( x -> "b." + x + " AS b" + x ).collect( Collectors.toList() );
            allColumns.addAll( HOST_TABLE_COLS.stream().map( x -> "h." + x + " AS h" + x ).toList() );

            try ( final PreparedStatement stmt = createPreparedQueryClause( "SELECT " + Strings.join( ",", allColumns ) + " FROM " + BUILDS_TABLE + " b INNER JOIN " + HOSTS_TABLE + " h USING (host_id)", criteria, con ); )
            {
                final List<Build> result = new ArrayList<>();
                final BuildRowMapper mapper = new BuildRowMapper( "b", "h" );
                try ( final ResultSet rs = stmt.executeQuery() )
                {
                    int row = 1;
                    while (rs.next())
                    {
                        result.add( mapper.mapRow( rs, row++ ) );
                    }
                }
                return result;
            }
        } );
    }

    public List<Build> getBuilds(Set<Long> ids)
    {
        Validate.isTrue( CollectionUtils.isNotEmpty( ids ) );

        return jdbcTemplate.execute( (ConnectionCallback<List<Build>>) con ->
        {
            final List<String> allColumns = BUILD_TABLE_COLS.stream().map( x -> "b." + x + " AS b" + x ).collect( Collectors.toList() );
            allColumns.addAll( HOST_TABLE_COLS.stream().map( x -> "h." + x + " AS h" + x ).toList() );

            final String idString = ids.stream().map( x -> Long.toString( x ) ).collect( Collectors.joining( "," ) );
            final String sql = "SELECT " + Strings.join( ",", allColumns ) + " FROM " + BUILDS_TABLE + " b INNER JOIN " + HOSTS_TABLE + " h USING (host_id) WHERE b.build_id IN (" + idString + ")";
            try ( final PreparedStatement stmt = con.prepareStatement( sql ) )
            {
                final List<Build> result = new ArrayList<>();
                final BuildRowMapper mapper = new BuildRowMapper( "b", "h" );
                try ( final ResultSet rs = stmt.executeQuery() )
                {
                    int row = 1;
                    while (rs.next())
                    {
                        result.add( mapper.mapRow( rs, row++ ) );
                    }
                }
                return result;
            }
        } );
    }

    public List<Record> getRecords(long buildId)
    {
        return jdbcTemplate.query( "SELECT * FROM " + RECORDS_TABLE + " WHERE build_id=" + buildId, new RecordMapper() );
    }

    public Map<Long, List<Record>> getRecords(Set<Long> buildIds)
    {
        final String ids = buildIds.stream().map( x -> Long.toString( x ) ).collect( Collectors.joining( "," ) );
        final List<Record> list = jdbcTemplate.query( "SELECT * FROM " + RECORDS_TABLE + " WHERE build_id IN (" + ids + ")", new RecordMapper() );
        return list.stream().collect( Collectors.groupingBy( x -> x.buildId ) );
    }

    public void saveRecords(List<Record> toInsert)
    {
        Validate.notNull( toInsert, "toInsert must not be null" );
        Validate.isTrue( toInsert.stream().noneMatch( x -> x.id != 0 ), "this method can only persist new instances" );
        if ( !toInsert.isEmpty() )
        {
            final List<String> RECORD_COLS = List.of( "build_id", "phase_id", "plugin_artifact_id", "plugin_version",
                "artifact_id", "artifact_version", "start_time", "end_time" );

            jdbcTemplate.execute( (ConnectionCallback<Void>) con ->
            {
                final String cols = RECORD_COLS.stream().map( x -> '"' + x + '"' ).collect( Collectors.joining( "," ) );
                final String placeholders = RECORD_COLS.stream().map( x -> "?" ).collect( Collectors.joining( "," ) );
                final String sql = "INSERT INTO " + RECORDS_TABLE + " (" + cols + ") VALUES (" + placeholders + ")";

                try ( final PreparedStatement stmt = con.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS ) )
                {
                    for ( Record record : toInsert )
                    {
                        int y = 1;
                        stmt.setLong( y++, record.buildId );
                        stmt.setLong( y++, record.phaseId );
                        stmt.setLong( y++, record.pluginArtifactId );
                        stmt.setString( y++, record.pluginVersion );
                        stmt.setLong( y++, record.artifactId );
                        stmt.setString( y++, record.artifactVersion );
                        stmt.setTimestamp( y++, toTimestamp( record.startTime ) );
                        stmt.setTimestamp( y++, toTimestamp( record.endTime ) );
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                    try ( ResultSet rs = stmt.getGeneratedKeys() )
                    {
                        int count = 0;
                        for ( final Iterator<Record> it = toInsert.iterator(); rs.next(); )
                        {
                            it.next().id = rs.getLong( 1 );
                            count++;
                        }
                        if ( count != toInsert.size() )
                        {
                            throw new IllegalStateException( "Internal error, received wrong number of generated keys?" );
                        }
                    }
                }
                return null;
            } );
        }
    }

    public List<Artifact> getArtifacts(Set<ArtifactId> ids)
    {
        Validate.notNull( ids, "ids must not be null" );
        if ( ids.isEmpty() )
        {
            return new ArrayList<>();
        }
        return jdbcTemplate.execute( (ConnectionCallback<List<Artifact>>) con ->
        {
            final List<Artifact> result = new ArrayList<>();

            final String sql = "SELECT * from " + ARTIFACTS_TABLE + " WHERE "
                + ids.stream().map( x -> "(group_id_txt=? AND artifact_id_txt=?)" ).collect( Collectors.joining( " OR " ) );
            try ( PreparedStatement stmt = con.prepareStatement( sql ) )
            {
                int paramNo = 1;
                for ( final ArtifactId id : ids )
                {
                    stmt.setString( paramNo++, id.groupIdText() );
                    stmt.setString( paramNo++, id.artifactIdText() );
                    stmt.addBatch();
                }
                try ( ResultSet rs = stmt.executeQuery() )
                {
                    while (rs.next())
                    {
                        final Artifact r = new Artifact();
                        r.id = rs.getLong( "artifact_id" );
                        r.groupId = rs.getString( "group_id_txt" );
                        r.artifactId = rs.getString( "artifact_id_txt" );
                        result.add( r );
                    }
                }
            }
            return result;
        } );
    }

    public List<Artifact> getArtifactsByIDs(Set<Long> ids)
    {
        Validate.isTrue( CollectionUtils.isNotEmpty( ids ) );
        final String idString = ids.stream().map( x -> Long.toString( x ) ).collect( Collectors.joining( "," ) );
        final String sql = "SELECT * FROM " + ARTIFACTS_TABLE + " WHERE artifact_id IN (" + idString + ")";
        return jdbcTemplate.query( sql, new ArtifactMapper() );
    }

    public void saveNewArtifacts(List<Artifact> toInsert)
    {
        Validate.notNull( toInsert, "toInsert must not be null" );
        Validate.isTrue( toInsert.stream().noneMatch( x -> x.id != 0 ), "this method can only persist new instances" );
        if ( !toInsert.isEmpty() )
        {
            jdbcTemplate.execute( (ConnectionCallback<Void>) con -> {
                final String sql = "INSERT INTO " + ARTIFACTS_TABLE + " (group_id_txt,artifact_id_txt) VALUES (?,?)";

                try ( final PreparedStatement stmt = con.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS ) )
                {
                    for ( Artifact artifact : toInsert )
                    {
                        stmt.setString( 1, artifact.groupId );
                        stmt.setString( 2, artifact.artifactId );
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                    try ( ResultSet rs = stmt.getGeneratedKeys() )
                    {
                        int count = 0;
                        for ( final Iterator<Artifact> it = toInsert.iterator(); rs.next(); )
                        {
                            it.next().id = rs.getLong( 1 );
                            count++;
                        }
                        if ( count != toInsert.size() )
                        {
                            throw new IllegalStateException( "Internal error, received wrong number of generated keys?" );
                        }
                    }
                }
                return null;
            } );
        }
    }

    private static final class LifecycleMapper implements RowMapper<LifecyclePhase>
    {

        @Override
        public LifecyclePhase mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            final LifecyclePhase result = new LifecyclePhase();
            result.phaseId = rs.getLong( "phase_id" );
            result.name = rs.getString( "phase_name" );
            return result;
        }
    }

    public Map<String, LifecyclePhase> getPhases(Set<String> names)
    {
        Validate.notNull( names, "names must not be null" );
        if ( names.isEmpty() )
        {
            return new HashMap<>();
        }
        final String sql = "SELECT * from " + PHASES_TABLE + " WHERE phase_name IN ("
            + names.stream().map( x -> "'" + x + "'" ).collect( Collectors.joining( ", " ) ) + ")";
        return jdbcTemplate.queryForStream( sql, new LifecycleMapper() ).collect( Collectors.toMap( x -> x.name, y -> y ) );
    }

    public List<LifecyclePhase> getPhases()
    {
        final String sql = "SELECT * from " + PHASES_TABLE;
        return jdbcTemplate.queryForStream( sql, new LifecycleMapper() ).collect( Collectors.toList() );
    }

    public List<LifecyclePhase> getPhasesByIDs(Set<Long> phaseIds)
    {
        final String ids = phaseIds.stream().map( x -> Long.toString( x ) ).collect( Collectors.joining( "," ) );
        final String sql = "SELECT * from " + PHASES_TABLE + " WHERE phase_id IN (" + ids + ")";
        return jdbcTemplate.queryForStream( sql, new LifecycleMapper() ).collect( Collectors.toList() );
    }

    public void saveNewPhases(List<LifecyclePhase> phases)
    {

        jdbcTemplate.execute( (ConnectionCallback<Void>) con -> {
            final String sql = "INSERT INTO " + PHASES_TABLE + " (phase_name) VALUES (?)";

            try ( final PreparedStatement stmt = con.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS ) )
            {
                for ( LifecyclePhase phase : phases )
                {
                    stmt.setString( 1, phase.name );
                    stmt.addBatch();
                }
                stmt.executeBatch();
                try ( ResultSet rs = stmt.getGeneratedKeys() )
                {
                    int count = 0;
                    for ( final Iterator<LifecyclePhase> it = phases.iterator(); rs.next(); )
                    {
                        it.next().phaseId = rs.getLong( 1 );
                        count++;
                    }
                    if ( count != phases.size() )
                    {
                        throw new IllegalStateException( "Internal error, received wrong number of generated keys?" );
                    }
                }
            }
            return null;
        } );
    }

    public void saveNew(List<Build> toInsert)
    {
        Validate.notNull( toInsert, "list must not be null" );
        Validate.isTrue( toInsert.stream().noneMatch( x -> x.id != 0 ), "Method must only be called to persist NEW instances" );

        // INSERT
        if ( !toInsert.isEmpty() )
        {
            final Predicate<String> notIdColumn = Predicate.not( BUILD_ID_COL::equals );
            final List<String> nonIdColumns = BUILD_TABLE_COLS.stream().filter( notIdColumn ).toList();
            final String cols = String.join( ",", nonIdColumns );
            final String placeHolders = nonIdColumns.stream().map( x -> "?" ).collect( Collectors.joining( "," ) );
            final String sql = "INSERT INTO " + BUILDS_TABLE + " (" + cols + ") VALUES (" + placeHolders + ")";

            jdbcTemplate.execute( (ConnectionCallback<Void>) con -> {
                try ( final PreparedStatement stmt = con.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS ) )
                {
                    for ( Build build : toInsert )
                    {
                        int y = 1;
                        stmt.setTimestamp( y++, toTimestamp( build.startTime ) );
                        stmt.setInt( y++, (int) build.duration.toMillis() );
                        stmt.setLong( y++, build.host.hostId );
                        stmt.setString( y++, build.projectName );
                        stmt.setString( y++, build.branchName );
                        stmt.setInt( y++, build.maxConcurrency );
                        stmt.setString( y++, build.jvmVersion );
                        stmt.setInt( y++, build.availableProcessors );
                        stmt.setString( y++, build.gitHash );
                        stmt.setString( y++, build.systemProperties );
                        stmt.setString( y++, build.environmentProperties );
                        if ( (y - 1) != nonIdColumns.size() )
                        {
                            throw new RuntimeException( "Internal error, prepared parameter count " + (y - 1) + " does not match placeholder count " + nonIdColumns.size() );
                        }
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                    try ( ResultSet rs = stmt.getGeneratedKeys() )
                    {
                        int count = 0;
                        for ( final Iterator<Build> it = toInsert.iterator(); rs.next(); )
                        {
                            it.next().id = rs.getLong( 1 );
                            count++;
                        }
                        if ( count != toInsert.size() )
                        {
                            throw new IllegalStateException( "Internal error, received wrong number of generated keys?" );
                        }
                    }
                }
                return null;
            } );
        }
    }

    public interface StatementSetter
    {
        void apply(PreparedStatement stmt, int paramInde) throws SQLException;
    }

    protected static PreparedStatement createPreparedQueryClause(String baseQuery,
                                                                 SearchCriteria criteria,
                                                                 Connection con) throws SQLException
    {
        boolean hostsTableAlreadyJoined = baseQuery.toLowerCase().contains( ("INNER JOIN " + HOSTS_TABLE + " h").toLowerCase() );

        final List<String> constraints = new ArrayList<>();
        final List<StatementSetter> params = new ArrayList<>();

        if ( StringUtils.isNotBlank( criteria.projectName ) )
        {
            constraints.add( "project_name=?" );
            params.add( (stmt, idx) -> stmt.setString( idx, criteria.projectName ) );
        }

        if ( criteria.interval != null )
        {
            constraints.add( "(build_start_time>=? AND build_start_time < ?)" );
            params.add( (stmt, idx) -> stmt.setTimestamp( idx, criteria.interval.startAsTimestamp() ) );
            params.add( (stmt, idx) -> stmt.setTimestamp( idx, criteria.interval.endAsTimestamp() ) );
        }

        if ( criteria.branchName != null )
        {
            constraints.add( "branch_name = ?" );
            params.add( (stmt, idx) -> stmt.setString( idx, criteria.branchName ) );
        }

        if ( CollectionUtils.isNotEmpty( criteria.phases ) )
        {

            final String ids =
                criteria.phases.stream().map( x -> Long.toString( x.phaseId ) ).collect( Collectors.joining( "," ) );
            baseQuery += " INNER JOIN " + RECORDS_TABLE + " r USING (build_id)";
            constraints.add( "r.phase_id IN (" + ids + ")" );
        }

        if ( criteria.host != null )
        {
            constraints.add( "host_id=" + criteria.host.hostId );
        }

        final String whereClause;
        if ( constraints.isEmpty() )
        {
            whereClause = "";
        }
        else
        {
            whereClause = " WHERE " + String.join( " AND ", constraints );
        }

        if ( criteria.sortColumn == SearchCriteria.SortColumn.HOST )
        {
            if ( !hostsTableAlreadyJoined )
            {
                baseQuery += " INNER JOIN " + HOSTS_TABLE + " h USING (host_id)";
                hostsTableAlreadyJoined = true;
            }
        }

        String sql = baseQuery + whereClause;

        if ( criteria.sortColumn != null )
        {
            final String colname = switch( criteria.sortColumn )
            {
                case BUILD_TIMESTAMP -> "build_start_time";
                case PROJECT_NAME -> "project_name";
                case BRANCH_NAME -> "branch_name";
                case HOST -> "COALESCE(host_name,host_ip::text)";
                case BUILD_DURATION -> "build_duration_millis";
            };
            final String dir = criteria.sortAscending ? "ASC" : "DESC";
            sql += " ORDER BY " + colname + " " + dir;
            if ( criteria.offset > 0 )
            {
                sql += " OFFSET " + criteria.offset;
            }
            if ( criteria.limit > 0 )
            {
                sql += " LIMIT " + criteria.limit;
            }
        }

        LOG.debug( "sql = " + sql );
        final PreparedStatement stmt = con.prepareStatement( sql );
        for ( int i = 0; i < params.size(); i++ )
        {
            params.get( i ).apply( stmt, i + 1 );
        }
        return stmt;
    }

    public List<String> getDistinctProjects()
    {
        final String sql = "SELECT DISTINCT project_name FROM " + BUILDS_TABLE;
        return jdbcTemplate.queryForList( sql, String.class );
    }

    public List<String> getBranchNames(String projectName, Host host)
    {
        String sql = "SELECT DISTINCT branch_name FROM " + BUILDS_TABLE + " WHERE project_name=?";
        if ( host != null )
        {
            sql += " AND host_id=" + host.hostId;
        }
        return jdbcTemplate.queryForList( sql, String.class, projectName );
    }

    public List<Host> getHosts()
    {
        final String sql = "SELECT * FROM "+HOSTS_TABLE;
        return jdbcTemplate.query( sql, new HostsMapper() );
    }

    public Optional<Host> getHostByIP(InetAddress hostIP) {
        Validate.notNull( hostIP, "hostIP must not be null" );
        final String sql = "SELECT * FROM "+HOSTS_TABLE+" WHERE host_ip=?::inet";
        final List<Host> list = jdbcTemplate.query( sql, new HostsMapper(), hostIP.getHostAddress() );
        return switch(list.size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of( list.get( 0 ) );
            default -> throw new IncorrectResultSizeDataAccessException( "Expected one host with IP "+hostIP+" but found "+list.size(), 1 );
        };
    }

    void saveOrUpdate(Host host)
    {
        Validate.notNull( host, "host must not be null" );
        if ( host.hostId == 0 ) {
            final String sql = "INSERT INTO " + HOSTS_TABLE + " (host_ip,host_name) VALUES (?::inet,?)";
            final KeyHolder keyHolder = new GeneratedKeyHolder();
            final PreparedStatementCreator psc = con -> {
                final PreparedStatement stmt = con.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
                stmt.setString( 1, host.getHostIP().getHostAddress() );
                stmt.setString( 2, host.getHostName().orElse( null ) );
                return stmt;
            };

            jdbcTemplate.update( psc, keyHolder );
            final Number id = (Number) keyHolder.getKeys().get( "host_id" );
            host.hostId = id.longValue();
        } else {
            final String sql = "UPDATE " + HOSTS_TABLE + " SET host_name=?, host_ip=?::inet WHERE host_id=?";
            jdbcTemplate.update( sql, host.getHostName().orElse( null ), host.getHostIP().getHostAddress(), host.hostId );
        }
    }

    private static java.sql.Timestamp toTimestamp(ZonedDateTime dt)
    {
        if ( dt == null ) {
            return null;
        }
        return new Timestamp( dt.toInstant().toEpochMilli() );
    }
}