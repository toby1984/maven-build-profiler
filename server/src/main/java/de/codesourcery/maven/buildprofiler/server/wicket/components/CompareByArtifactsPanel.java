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
package de.codesourcery.maven.buildprofiler.server.wicket.components;

import de.codesourcery.maven.buildprofiler.server.db.DAO;
import de.codesourcery.maven.buildprofiler.server.db.DbService;
import de.codesourcery.maven.buildprofiler.server.model.Artifact;
import de.codesourcery.maven.buildprofiler.server.model.ArtifactId;
import de.codesourcery.maven.buildprofiler.server.model.Build;
import de.codesourcery.maven.buildprofiler.server.model.Record;
import de.codesourcery.maven.buildprofiler.server.wicket.CompareByPhasesPage;
import de.codesourcery.maven.buildprofiler.server.wicket.IWicketUtils;
import de.codesourcery.maven.buildprofiler.server.wicket.Utils;
import de.codesourcery.maven.buildprofiler.server.wicket.components.datatable.MyDataTable;
import de.codesourcery.maven.buildprofiler.server.wicket.components.tooltip.TooltipBehaviour;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CompareByArtifactsPanel extends Panel implements IWicketUtils
{
    /**
     * Percentage value (0...100) that will make an execution time being
     * rendered as 'slower' (orange) when it is slower than the fastest
     * execution time.
     */
    protected static final int SLOWER_PERCENTAGE_THRESHOLD = 20;

    private final List<Build> toCompare;
    private final DAO.SearchCriteria criteria;
    private final String phase;

    protected record DurationAndArtifactVersion(Duration duration, String artifactVersion) implements Serializable {
        protected DurationAndArtifactVersion
        {
            Validate.notNull( duration, "duration must not be null" );
        }
    }

    public final class RowWrapper implements Serializable
    {
        public final ArtifactId artifactId;
        // CAREFUL: The following list will contain NULL values for every build that did not
        //          have this row's artifactId
        private final List<DurationAndArtifactVersion> executionTimes;

        private final int fastestExecTimeIdx; // index into List<Duration> list
        private final int slowestExecTimeIdx; // index into List<Duration> list

        public RowWrapper(ArtifactId artifactId,List<DurationAndArtifactVersion> executionTimes) {
            Validate.notNull( artifactId, "artifactId must not be null" );
            Validate.notNull( executionTimes, "executionTimes must not be null" );
            this.artifactId = artifactId;
            this.executionTimes = executionTimes;

            int fastIdx = -1;
            int slowIdx = -1;
            Duration bestTime = null;
            Duration worstTime = null;
            for ( int i = 0, executionTimesSize = executionTimes.size(); i < executionTimesSize; i++ )
            {
                final DurationAndArtifactVersion listItem = executionTimes.get( i );
                if ( listItem != null )
                {
                    final Duration time = listItem.duration;
                    if ( bestTime == null || time.compareTo( bestTime ) < 0 )
                    {
                        bestTime = time;
                        fastIdx = i;
                    }
                    if ( worstTime == null || time.compareTo( worstTime ) > 0 )
                    {
                        worstTime = time;
                        slowIdx = i;
                    }
                }
            }
            this.fastestExecTimeIdx = fastIdx;
            this.slowestExecTimeIdx = slowIdx;
        }

        /**
         * Returns the record for a given build index.
         * @param buildIndex
         * @return record or <code>null</code> if the build at the
         * given <code>buildIndex</code> did not have an artifact matching the current {@link #artifactId}.
         */
        public Optional<DurationAndArtifactVersion> getRecord(int buildIndex) {
            return Optional.ofNullable( executionTimes.get( buildIndex ) );
        }

        public Optional<Duration> getFastest() {
            return this.executionTimes.stream().filter( Objects::nonNull ).map( DurationAndArtifactVersion::duration ).reduce( (a, b) -> a.compareTo( b ) <= 0 ? a : b );
        }

        public boolean isFastest(int durationIdx) {
            return fastestExecTimeIdx == durationIdx;
        }

        public boolean isSlowest(int durationIdx) {
            return slowestExecTimeIdx == durationIdx;
        }

        public Optional<Float> getPercentageDeltaToFastest(int durationIdx) {
            final Optional<Duration> current = getRecord( durationIdx ).map( DurationAndArtifactVersion::duration );
            final Optional<Duration> fastest = getFastest();
            if ( current.isEmpty() || fastest.isEmpty() ) {
                return Optional.empty();
            }
            return Optional.of( (100f*(current.get().toMillis() / (float) fastest.get().toMillis()))-100f );
        }
    }

    @SpringBean
    private DbService db;

    private final MyDataProvider dataProvider;

    private final class MyDataProvider extends SortableDataProvider<RowWrapper,String>
    {
        private List<RowWrapper> data;

        @Override
        public void detach()
        {
            data  = null;
        }

        @Override
        public Iterator<? extends RowWrapper> iterator(long first, long count)
        {
            return load().iterator();
        }

        private List<RowWrapper> load()
        {
            if ( data == null ) {
                final Map<Long, List<Record>> recordsByBuildID =
                    db.getRecords( toCompare.stream().map( x -> x.id ).collect( Collectors.toSet() ), DbService.Grouping.ARTIFACT );

                // gather all possible artifacts
                final Set<Long> ids = recordsByBuildID.values().stream()
                    .flatMap( Collection::stream ).map( x -> x.artifactId ).collect( Collectors.toSet() );

                final List<Artifact> artifacts = db.getArtifactsByIDs( ids );

                final Map<Long, Artifact> artifactsByID = artifacts.stream().collect( Collectors.toMap( x -> x.id, y -> y ) );

                final Set<ArtifactId> artifactIds = artifacts.stream().map( Artifact::toArtifactId ).collect( Collectors.toSet());
                final List<ArtifactId> sortedArtifactIds = new ArrayList<>( artifactIds );
                sortedArtifactIds.sort( Comparator.comparing( ArtifactId::groupIdText ).thenComparing( ArtifactId::artifactIdText ) );

                final List<RowWrapper> wrappers = new ArrayList<>();
                for ( final ArtifactId id : sortedArtifactIds )
                {
                    final List<DurationAndArtifactVersion> timesPerBuildAndArtifact = new ArrayList<>();
                    final Predicate<Record> matchesArtifact = record -> {
                        final Artifact artifact = artifactsByID.get( record.artifactId );
                        return id.matches( artifact );
                    };
                    for ( final Build build : toCompare )
                    {
                        // sum duration of all artifacts for the given phase (if possible)
                        final List<Record> records = recordsByBuildID.get( build.id );

                        final long[] result = records.stream().filter( matchesArtifact )
                            .mapToLong( x -> x.duration().toMillis() ).toArray();

                        if ( result.length > 0 )
                        {
                            final Duration sumDuration = Duration.ofMillis(Arrays.stream(result).sum());
                            timesPerBuildAndArtifact.add(new DurationAndArtifactVersion(sumDuration, records.stream().filter(matchesArtifact).findFirst().get().artifactVersion));
                        } else {
                            timesPerBuildAndArtifact.add( null );
                        }
                    }
                    wrappers.add( new RowWrapper( id, timesPerBuildAndArtifact ) );
                }
                data =  wrappers;
            }
            return data;
        }

        @Override
        public long size()
        {
            return load().size();
        }

        @Override
        public IModel<RowWrapper> model(RowWrapper object)
        {
            return Model.of( object );
        }
    }

    public CompareByArtifactsPanel(String id, List<Build> toCompare, DAO.SearchCriteria criteria, String phase)
    {
        super( id );
        Validate.notNull( toCompare, "toCompare must not be null" );
        Validate.notNull( criteria, "criteria must not be null" );
        Validate.notBlank( phase, "phase must not be null or blank");

        this.criteria = criteria;
        this.toCompare = toCompare;
        this.phase = phase;
        this.dataProvider = new MyDataProvider();
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        final List<IColumn<RowWrapper, String>> columns = createColumns();
        final MyDataTable<RowWrapper, String> table = new MyDataTable<>( "compareTable", columns, dataProvider, 10000 );
        add( table );

        add( new Label( "title", "Comparing phase '%s' by artifacts".formatted( phase ) ) );

        final Form<Void> dummyForm = new Form<>( "dummy" );
        add( dummyForm );

        final Button b = new Button( "backButton" ) {
            @Override
            public void onSubmit()
            {
                setResponsePage( new CompareByPhasesPage( toCompare, criteria ) );
            }
        };
        b.setDefaultFormProcessing( false );
        dummyForm.add( b );
    }

    private List<IColumn<RowWrapper, String>> createColumns()
    {
        final List<IColumn<RowWrapper, String>> result = new ArrayList<>();

        result.add( new LambdaColumn<>( Model.of( "Artifact" ), x -> x.artifactId.toString() ) );

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.s" );
        for ( int idx = 0, objectSize = toCompare.size(); idx < objectSize; idx++ )
        {
            final Build build = toCompare.get( idx );
            final int finalIdx = idx;
            result.add( new LambdaColumn<>( Model.of( formatter.format( build.startTime ) ), x ->
            {
                final Optional<Duration> record = x.getRecord( finalIdx ).map( DurationAndArtifactVersion::duration );
                String msg = record.map(Utils::formatDuration).orElse("n/a");
                if ( record.isPresent() && x.isSlowest( finalIdx ) ) {
                    final Optional<Duration> fastest = x.getFastest();
                    if ( fastest.isPresent() )
                    {
                        final float perc = (100f*(record.get().toMillis() / (float) fastest.get().toMillis()))-100f;
                        final DecimalFormat DF = new DecimalFormat( "#####0.#" );
                        msg = "+" + DF.format( perc ) + " % (" + msg + ")";
                    }
                }
                return msg;
            } )
            {
                @Override
                public void populateItem(Item<ICellPopulator<RowWrapper>> item, String componentId, IModel<RowWrapper> rowModel)
                {
                    final Label label = new Label( componentId, getDataModel( rowModel ) );
                    final RowWrapper wrapper = rowModel.getObject();
                    if ( wrapper.isFastest( finalIdx ) ) {
                        label.add( new AttributeModifier( "class", "fastest" ) );
                    } else if ( wrapper.isSlowest( finalIdx ) ) {
                        final Optional<Float> delta = wrapper.getPercentageDeltaToFastest( finalIdx );
                        delta.ifPresent( d ->
                        {
                            if ( d > 0 )
                            {
                                if ( d < SLOWER_PERCENTAGE_THRESHOLD )
                                {
                                    label.add( new AttributeModifier( "class", "slower" ) );
                                } else if ( wrapper.isSlowest( finalIdx ) ) {
                                    label.add( new AttributeModifier( "class", "slowest" ) );
                                }
                            }
                        });
                    }
                    final IModel<String> ttModel = rowModel.map( x -> {
                        final String version = x.executionTimes.isEmpty() ? "n/a" : x.getRecord( 0 ).map( y -> y.artifactVersion ).orElse( "n/a" );
                        return """
                            <table>
                              <tr><td>groupId:</td><td>%s</td></tr>
                              <tr><td>artifactId:</td><td>%s</td></tr>
                              <tr><td>version:</td><td>%s</td></tr>
                            </table>
                            """.formatted( sanitize( x.artifactId.groupIdText() ), sanitize(x.artifactId.artifactIdText()), sanitize(version));
                    });
                    label.add( TooltipBehaviour.of(ttModel, 350).setEscapeModelStrings(false));
                    item.add( label );
                }

                @Override
                public Component getHeader(String componentId)
                {
                    final Component result = super.getHeader( componentId );
                    final IModel<String> ttModel = () -> {
                        final String msg = """
                            <table>
                              <tr><td>Project:</td><td>%s</td></tr>
                              <tr><td>Branch:</td><td>%s</td></tr>
                              <tr><td>Host:</td><td>%s</td></tr>
                              <tr><td>GIT hash:</td><td>%s</td></tr>
                            </table>""";
                        return msg.formatted( sanitize( build.projectName ),
                            sanitize( build.branchName ),
                            sanitize( build.host.getHostName().orElseGet( () -> build.host.getHostIP().getHostAddress() ) ),
                            sanitize( build.gitHash == null ? "n/a" : build.gitHash ) );
                    };
                    result.add( TooltipBehaviour.of( ttModel, 450 ).setEscapeModelStrings( false ) );
                    return result;
                }
            });
        }
        return result;
    }
}
