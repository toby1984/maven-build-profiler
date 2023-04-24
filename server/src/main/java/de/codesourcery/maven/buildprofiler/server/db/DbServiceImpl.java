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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.codesourcery.maven.buildprofiler.common.BuildResult;
import de.codesourcery.maven.buildprofiler.server.LongInterval;
import de.codesourcery.maven.buildprofiler.server.model.Artifact;
import de.codesourcery.maven.buildprofiler.server.model.ArtifactId;
import de.codesourcery.maven.buildprofiler.server.model.Build;
import de.codesourcery.maven.buildprofiler.server.model.Host;
import de.codesourcery.maven.buildprofiler.server.model.LifecyclePhase;
import de.codesourcery.maven.buildprofiler.server.model.Record;
import de.codesourcery.maven.buildprofiler.shared.ArtifactCoords;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class DbServiceImpl implements DbService
{
    // TODO: Maybe get that from Spring as well ?
    private final ObjectMapper mapper = new ObjectMapper();
    private DAO dao;

    @Override
    @Transactional
    public List<Record> getRecords(long buildId)
    {
        return dao.getRecords( buildId );
    }

    @Override
    @Transactional
    public Map<Long, List<Record>> getRecords(Set<Long> buildIds, Grouping grouping)
    {
        Validate.notNull( buildIds, "buildIds must not be null" );
        Validate.notNull( grouping, "grouping must not be null" );
        final Map<Long, List<Record>> map = dao.getRecords( buildIds );
        map.values().forEach( list -> performGrouping( list, grouping ) );
        return map;
    }

    // CAREFUL - mutates input list !!
    static void performGrouping(List<Record> list, Grouping grouping) {

        Validate.notNull( list, "list must not be null" );
        Validate.notNull( grouping, "grouping must not be null" );

        if ( list.size() < 2 ) {
            return;
        }

        final Map<Long, List<Record>>  grouped = switch( grouping )
        {
            case PHASE -> list.stream().collect( Collectors.groupingBy( x -> x.phaseId ) );
            case ARTIFACT -> list.stream().collect( Collectors.groupingBy( x -> x.artifactId ) );
            case PLUGIN -> list.stream().collect( Collectors.groupingBy( x -> x.pluginArtifactId ) );
            case NONE -> null;
        };
        if ( grouped == null ) {
            return;
        }
        list.clear();
        grouped.forEach( (id,toBeMerged) -> {
            final List<LongInterval> intervals = toBeMerged.stream().map( DbServiceImpl::interval ).collect( Collectors.toList() );
            final OptionalLong durationMillis = LongInterval.mergeIfPossible( intervals ).stream().mapToLong( LongInterval::length ).reduce( Long::sum );
            final Record r = toBeMerged.get( 0 );
            r.setDurationMillis( durationMillis.getAsLong() );
            list.add( r );
        });
    }

    private static LongInterval interval(Record r) {
        return LongInterval.of( r.startTime.toInstant().toEpochMilli(), r.endTime.toInstant().toEpochMilli() );
    }

    @Override
    @Transactional
    public void saveRecords(List<Record> toInsert)
    {
        dao.saveRecords( toInsert );
    }

    @Override
    @Transactional
    public List<Artifact> getArtifacts(Set<ArtifactId> ids)
    {
        return dao.getArtifacts( ids );
    }

    @Override
    @Transactional
    public List<Artifact> getArtifactsByIDs(Set<Long> ids)
    {
        return dao.getArtifactsByIDs( ids );
    }

    @Override
    @Transactional
    public void saveNewArtifacts(List<Artifact> toInsert)
    {
        dao.saveNewArtifacts( toInsert );
    }

    @Override
    @Transactional
    public Map<String, LifecyclePhase> getPhases(Set<String> names)
    {
        return dao.getPhases( names );
    }

    @Override
    @Transactional
    public List<LifecyclePhase> getPhases()
    {
        return dao.getPhases();
    }

    @Override
    @Transactional
    public List<LifecyclePhase> getPhasesByIDs(Set<Long> phaseIds)
    {
        return dao.getPhasesByIDs( phaseIds );
    }

    @Override
    @Transactional
    public void saveNewPhases(List<LifecyclePhase> phases)
    {
        dao.saveNewPhases( phases );
    }

    @Override
    @Transactional
    public int getBuildCount(DAO.SearchCriteria criteria)
    {
        return dao.getBuildCount( criteria );
    }

    @Override
    @Transactional
    public List<Build> getBuild(DAO.SearchCriteria criteria)
    {
        return dao.getBuild( criteria );
    }

    @Override
    @Transactional
    public List<Build> getBuilds(Set<Long> ids)
    {
        return dao.getBuilds( ids );
    }

    @Override
    @Transactional
    public void saveNew(List<Build> toInsert)
    {
        dao.saveNew( toInsert );
    }

    @Override
    @Transactional
    public List<String> getDistinctProjects()
    {
        return dao.getDistinctProjects();
    }

    @Override
    @Transactional
    public List<String> getBranchNames(String projectName, Host host)
    {
        return dao.getBranchNames(projectName, host);
    }

    @Override
    @Transactional
    public List<Host> getHosts()
    {
        return dao.getHosts();
    }

    @Override
    @Transactional
    public Optional<Host> getHostByIP(InetAddress hostIP)
    {
        Validate.notNull( hostIP, "hostIP must not be null" );
        return dao.getHostByIP( hostIP );
    }

    @Override
    @Transactional
    public void saveOrUpdate(Host host)
    {
        dao.saveOrUpdate( host );
    }

    @Override
    @Transactional
    public void save(BuildResult data) throws JsonProcessingException, UnknownHostException
    {
        final Build b = new Build();

        final InetAddress hostIP = InetAddress.getByName( data.hostIP );
        Host host = dao.getHostByIP( hostIP ).orElse( null );
        if ( host == null ) {
            host = new Host();
            host.setHostIP( hostIP );
            host.setHostName( data.hostName );
            dao.saveOrUpdate( host );
        }
        else if ( ! Objects.equals( host.getHostName().orElse( null ), data.hostName ) )
        {
            host.setHostName( data.hostName );
            dao.saveOrUpdate( host );
        }
        b.host = host;
        b.startTime = Instant.ofEpochMilli( data.buildStartTime ).atZone( ZoneId.systemDefault() );
        b.duration = Duration.ofMillis( data.buildDurationMillis );
        b.projectName = data.projectName;
        b.branchName = data.branchName;
        b.maxConcurrency = data.maxConcurrency;
        b.jvmVersion = data.jvmVersion;
        b.availableProcessors = data.availableProcessors;
        b.gitHash = data.gitHash;
        b.systemProperties = mapper.writeValueAsString( data.systemProperties );
        b.environmentProperties = mapper.writeValueAsString( data.environment );
        dao.saveNew( List.of(b) );

        final Set<String> requiredPhases = new HashSet<>();
        final Set<ArtifactId> artifactIds = new HashSet<>();
        for ( BuildResult.Record record : data.records )
        {
            artifactIds.add(ArtifactId.of(record.artifact(data)));
            artifactIds.add(ArtifactId.of(record.plugin(data)));
            requiredPhases.add( record.phase );
        }

        // get phases & insert any missing phases into DB
        final Map<String, LifecyclePhase> existingPhases = dao.getPhases( requiredPhases );
        if ( existingPhases.size() != requiredPhases.size() )
        {
            final List<LifecyclePhase> list = requiredPhases.stream().filter( Predicate.not( existingPhases::containsKey ) ).map( x -> {
                final LifecyclePhase p = new LifecyclePhase();
                p.name = x;
                return p;
            } ).toList();

            dao.saveNewPhases( list );
            list.forEach( item -> existingPhases.put( item.name, item ) );
        }

        // get artifacts & insert any missing artifacts into DB
        final Map<ArtifactId, Artifact> found = dao.getArtifacts( artifactIds )
            .stream().collect( Collectors.toMap( Artifact::toArtifactId, y->y) );

        final List<ArtifactId> missing = artifactIds.stream().filter( x -> ! found.containsKey( x ) ).toList();
        if ( ! missing.isEmpty() )
        {
            final List<Artifact> toInsert = missing.stream().map( x -> {
                final Artifact a = new Artifact();
                a.groupId = x.groupIdText();
                a.artifactId = x.artifactIdText();
                return a;
            } ).toList();
            dao.saveNewArtifacts( toInsert );
            toInsert.forEach( a -> found.put( a.toArtifactId(), a ) );
        }

        // store records
        final List<Record> records = new ArrayList<>();
        for ( final BuildResult.Record r : data.records )
        {
            final Record rec = new Record();
            rec.buildId = b.id;
            rec.phaseId = existingPhases.get( r.phase ).phaseId;

            ArtifactCoords coords = r.plugin(data);
            ArtifactId id = ArtifactId.of(coords);
            final Artifact pluginArtifact = found.get(id);
            rec.pluginArtifactId = pluginArtifact.id;
            rec.pluginVersion = coords.version();

            coords = r.artifact(data);
            id = ArtifactId.of(coords);
            final Artifact artifact = found.get(id);
            rec.artifactId = artifact.id;
            rec.artifactVersion = coords.version();

            rec.startTime = Instant.ofEpochMilli( r.startMillis ).atZone( ZoneId.systemDefault() );
            rec.endTime = Instant.ofEpochMilli( r.endMillis ).atZone( ZoneId.systemDefault() );
            records.add( rec );
        }
        dao.saveRecords( records );
    }

    @Autowired
    public void setDao(DAO dao)
    {
        this.dao = dao;
    }
}
