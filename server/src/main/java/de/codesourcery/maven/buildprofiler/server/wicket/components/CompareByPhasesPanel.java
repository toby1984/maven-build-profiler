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
import de.codesourcery.maven.buildprofiler.server.model.Build;
import de.codesourcery.maven.buildprofiler.server.model.LifecyclePhase;
import de.codesourcery.maven.buildprofiler.server.model.Phase;
import de.codesourcery.maven.buildprofiler.server.model.Record;
import de.codesourcery.maven.buildprofiler.server.wicket.CompareByArtifactsPage;
import de.codesourcery.maven.buildprofiler.server.wicket.HomePage;
import de.codesourcery.maven.buildprofiler.server.wicket.IWicketUtils;
import de.codesourcery.maven.buildprofiler.server.wicket.Utils;
import de.codesourcery.maven.buildprofiler.server.wicket.components.datatable.MyDataTable;
import de.codesourcery.maven.buildprofiler.server.wicket.components.tooltip.TooltipBehaviour;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.io.Serializable;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CompareByPhasesPanel extends Panel implements IWicketUtils
{
    private final IModel<List<Build>> model;
    private final DAO.SearchCriteria criteria;

    public final class RowWrapper implements Serializable
    {
        public final String phase;
        private final List<Duration> executionTimes;
        private final int fastestExecTimeIdx; // index into List<Duration> list
        private final int slowestExecTimeIdx; // index into List<Duration> list

        public RowWrapper(String phase,List<Duration> executionTimes) {
            Validate.notBlank( phase, "phase must not be null or blank");
            Validate.notNull( executionTimes, "executionTimes must not be null" );
            this.phase = phase;
            this.executionTimes = executionTimes;

            int fastIdx = -1;
            int slowIdx = -1;
            Duration bestTime = null;
            Duration worstTime = null;
            for ( int i = 0, executionTimesSize = executionTimes.size(); i < executionTimesSize; i++ )
            {
                final Duration time = executionTimes.get( i );
                if ( time != null )
                {
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
         * given <code>buildIndex</code> did not have an artifact matching the current {@link #phase}.
         */
        public Optional<Duration> getRecord(int buildIndex) {
            return Optional.ofNullable( executionTimes.get( buildIndex ) );
        }

        public boolean isFastest(int durationIdx) {
            return fastestExecTimeIdx == durationIdx;
        }

        public boolean isSlowest(int durationIdx) {
            return slowestExecTimeIdx == durationIdx;
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
                        db.getRecords( model.getObject().stream().map( x -> x.id ).collect( Collectors.toSet() ) );

                    // gather all possible phases
                    final Set<Long> phaseIDs = recordsByBuildID.values().stream()
                        .flatMap( Collection::stream ).map( x -> x.phaseId ).collect( Collectors.toSet() );

                    final List<LifecyclePhase> phases = Phase.sort( db.getPhasesByIDs( phaseIDs ) );

                    final List<RowWrapper> wrappers = new ArrayList<>();
                    for ( final LifecyclePhase phase : phases )
                    {
                        final List<Duration> timesPerBuildAndPhase = new ArrayList<>();
                        for ( final Build build : model.getObject() )
                        {
                            // sum duration of all artifacts for the given phase (if possible)
                            final List<Record> records = recordsByBuildID.get( build.id );
                            final long[] result = records.stream().filter( r -> r.phaseId == phase.phaseId )
                                .mapToLong( x -> x.duration.toMillis() ).toArray();

                            if ( result.length > 0 )
                            {
                                timesPerBuildAndPhase.add( Duration.ofMillis( Arrays.stream( result ).sum() ) );
                            } else {
                                timesPerBuildAndPhase.add( null );
                            }
                        }
                        wrappers.add( new RowWrapper( phase.name, timesPerBuildAndPhase ) );
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

    private abstract class LinkWithLabel<T> extends Fragment
    {
        private final IModel<T> dataModel;
        private final IModel<?> displayModel;
        private final Label label;

        public LinkWithLabel(String id,IModel<?> displayModel, IModel<T> dataModel)
        {
            super( id, "linkWithLabel", CompareByPhasesPanel.this );
            Validate.notNull( displayModel, "displayModel must not be null" );
            Validate.notNull( dataModel, "model must not be null" );
            this.dataModel = dataModel;
            this.displayModel = displayModel;

            final AjaxLink<T> link = new AjaxLink<>("link", dataModel ) {

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    LinkWithLabel.this.onClick( target, getModelObject() );
                }
            };
            add( link );
            label = new Label( "label", displayModel );
            link.add( label );
        }

        public Label getLabel()
        {
            return label;
        }

        protected abstract void onClick(AjaxRequestTarget target, T modelObject);
    }

    public CompareByPhasesPanel(String id, IModel<List<Build>> toCompare, DAO.SearchCriteria criteria)
    {
        super( id );
        this.criteria = criteria;
        Validate.notNull( toCompare, "toCompare must not be null" );
        Validate.notNull( criteria, "criteria must not be null" );
        this.model = toCompare;
        this.dataProvider = new MyDataProvider();
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        final List<IColumn<RowWrapper, String>> columns = createColumns();
        final MyDataTable<RowWrapper, String> table = new MyDataTable<>( "compareTable", columns, dataProvider, 10000 );
        add( table );

        final Form<Void> dummyForm = new Form<>( "dummy" );
        add( dummyForm );

        final Button b = new Button( "backButton" ) {
            @Override
            public void onSubmit()
            {
                setResponsePage( new HomePage( criteria ) );
            }
        };
        b.setDefaultFormProcessing( false );
        dummyForm.add( b );
    }

    private List<IColumn<RowWrapper, String>> createColumns()
    {
        final List<IColumn<RowWrapper, String>> result = new ArrayList<>();

        result.add( new LambdaColumn<>( Model.of( "Phase" ), x -> x.phase ) );

        final List<Build> object = model.getObject();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.s" );
        for ( int idx = 0, objectSize = object.size(); idx < objectSize; idx++ )
        {
            final Build build = object.get( idx );
            final int finalIdx = idx;
            result.add( new LambdaColumn<>( Model.of( "Build " + formatter.format( build.startTime ) ), x ->
            {
                return x.getRecord( finalIdx ).map(Utils::formatDuration).orElse("n/a");
            } )
            {
                @Override
                public void populateItem(Item<ICellPopulator<RowWrapper>> item, String componentId, IModel<RowWrapper> rowModel)
                {
                    final LinkWithLabel<RowWrapper> linkWithLabel =
                        new LinkWithLabel<>( componentId, getDataModel( rowModel ), rowModel ) {
                            @Override
                            protected void onClick(AjaxRequestTarget target, RowWrapper modelObject)
                            {
                                setResponsePage( new CompareByArtifactsPage(model.getObject(),criteria,modelObject.phase) );
                            }
                        };
                    final RowWrapper wrapper = rowModel.getObject();
                    if ( wrapper.isFastest( finalIdx ) ) {
                        linkWithLabel.getLabel().add( new AttributeModifier( "class", "fastest" ) );
                    } else if ( wrapper.isSlowest( finalIdx ) ) {
                        linkWithLabel.getLabel().add( new AttributeModifier( "class", "slowest" ) );
                    }
                    item.add( linkWithLabel );
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
