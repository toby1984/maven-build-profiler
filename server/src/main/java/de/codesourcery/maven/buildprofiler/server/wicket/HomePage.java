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
package de.codesourcery.maven.buildprofiler.server.wicket;

import de.codesourcery.maven.buildprofiler.common.Interval;
import de.codesourcery.maven.buildprofiler.server.db.DAO;
import de.codesourcery.maven.buildprofiler.server.db.DbService;
import de.codesourcery.maven.buildprofiler.server.model.Build;
import de.codesourcery.maven.buildprofiler.server.model.Host;
import de.codesourcery.maven.buildprofiler.server.wicket.components.LinkWithLabel;
import de.codesourcery.maven.buildprofiler.server.wicket.components.MyModalDialog;
import de.codesourcery.maven.buildprofiler.server.wicket.components.charts.DataSet;
import de.codesourcery.maven.buildprofiler.server.wicket.components.charts.DateXYDataItem;
import de.codesourcery.maven.buildprofiler.server.wicket.components.charts.LineChart;
import de.codesourcery.maven.buildprofiler.server.wicket.components.datatable.MyDataTable;
import de.codesourcery.maven.buildprofiler.server.wicket.components.tooltip.TooltipBehaviour;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.theme.DefaultTheme;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableFunction;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class HomePage extends AbstractBasePage
{
    // magic value indicating "Any Host"
    private static final Host ANY_HOST = new Host();
    // magic value indicating "Any branch"
    private static final String ANY_BRANCH = "Any Branch";

    private enum TableColumn {
        SELECTED_FOR_COMPARISON,
        BUILD_TIMESTAMP,
        PROJECT_NAME,
        BRANCH_NAME,
        HOST_NAME,
        BUILD_DURATION
    }

    @SpringBean
    private DbService db;

    private final Set<Long> buildIdsForComparison = new HashSet<>();

    private final DAO.SearchCriteria criteria;

    private final class MyDataProvider extends SortableDataProvider<Build, TableColumn>
    {
        // TODO: Cache results, implement detach()

        public List<? extends Build> list(long first, long count)
        {
            criteria.offset = (int) first;
            criteria.limit = (int) count;
            criteria.sortAscending = getSort().isAscending();
            criteria.sortColumn = switch( getSort().getProperty() ) {
                case SELECTED_FOR_COMPARISON -> throw new IllegalStateException( "Not supported for sorting" );
                case BUILD_TIMESTAMP -> DAO.SearchCriteria.SortColumn.BUILD_TIMESTAMP;
                case PROJECT_NAME -> DAO.SearchCriteria.SortColumn.PROJECT_NAME;
                case BRANCH_NAME -> DAO.SearchCriteria.SortColumn.BRANCH_NAME;
                case HOST_NAME -> DAO.SearchCriteria.SortColumn.HOST;
                case BUILD_DURATION -> DAO.SearchCriteria.SortColumn.BUILD_DURATION;
            };
            return db.getBuild( criteriaWithAnyValuesReplaced() );
        }

        @Override
        public Iterator<? extends Build> iterator(long first, long count)
        {
            return list( first, count ).iterator();
        }

        @Override
        public long size()
        {
            return db.getBuildCount( criteriaWithAnyValuesReplaced() );
        }

        private DAO.SearchCriteria criteriaWithAnyValuesReplaced() {
            // DB backend doesn't know about our ANY values
            // and expects NULL instead
            DAO.SearchCriteria result = criteria;
            if ( result.host == ANY_HOST ) {
                result = result.withHost( null );
            }
            //noinspection StringEquality
            if ( result.branchName == ANY_BRANCH ) {
                result = result.withBranchName( null );
            }
            return result;
        }

        @Override
        public IModel<Build> model(Build object)
        {
            return Model.of( object );
        }
    }

    private MyModalDialog dialog;
    private MyDataTable<Build, TableColumn> dataTable;
    private final MyDataProvider dataProvider = new MyDataProvider();

    private Button compareButton;

    public HomePage() {
        this( createDefaultCriteria() );
    }

    public HomePage(DAO.SearchCriteria criteria) {
        Validate.notNull( criteria, "criteria must not be null" );
        this.criteria = criteria;
    }

    private static DAO.SearchCriteria createDefaultCriteria() {
        final ZonedDateTime startDate = ZonedDateTime.now();
        final DAO.SearchCriteria criteria = new DAO.SearchCriteria();
        criteria.interval = new Interval( startDate.minusWeeks( 4 ), startDate );
        criteria.limit = 20;
        criteria.host = ANY_HOST;
        criteria.branchName = ANY_BRANCH;
        criteria.sortColumn = DAO.SearchCriteria.SortColumn.BUILD_TIMESTAMP;
        criteria.sortAscending = false;
        return criteria;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        dialog = new MyModalDialog( "modalWindow" );
        dialog.add(new DefaultTheme());
        dialog.trapFocus();
        dialog.closeOnEscape();
        add( dialog );

        final Form<Void> form = new Form<>( "form" );
        add( form );

        // table container
        final WebMarkupContainer tableContainer = new WebMarkupContainer( "tableContainer" );
        tableContainer.setOutputMarkupId( true );
        form.add( tableContainer );

        // line chart
        final IModel<DataSet<DateXYDataItem>> chartData = new IModel<>() {

            @Override
            public void detach()
            {
                dataProvider.detach();
            }

            @Override
            public DataSet<DateXYDataItem> getObject()
            {
                final long offset = dataTable.getCurrentPage() * dataTable.getItemsPerPage();
                final long count = Math.min( dataTable.getItemsPerPage(), dataProvider.size() - offset );
                final List<DateXYDataItem> items = dataProvider.list(offset, count ).stream().map( x -> new DateXYDataItem( x.startTime, x.duration.toMillis() ) ).toList();
                return new DataSet<>( items );
            }
        };

        final LineChart<DateXYDataItem> chart = new LineChart<>("linechart", chartData)
        {

            @Override
            protected String getXAxisLabelFor(DateXYDataItem x)
            {
                final DateTimeFormatter DF = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss" );
                return DF.format( x.x() );
            }

            @Override
            protected String getChartLabel()
            {
                return "Build time [ms]";
            }
        };
        tableContainer.add( chart );

        // data table
        final TableColumn sortCol = switch( criteria.sortColumn ) {
            case BUILD_TIMESTAMP -> TableColumn.BUILD_TIMESTAMP;
            case PROJECT_NAME -> TableColumn.PROJECT_NAME;
            case BRANCH_NAME -> TableColumn.BRANCH_NAME;
            case HOST -> TableColumn.HOST_NAME;
            case BUILD_DURATION -> TableColumn.BUILD_DURATION;
        };
        dataProvider.getSortState().setPropertySortOrder( sortCol, criteria.sortAscending ? SortOrder.ASCENDING : SortOrder.DESCENDING );

        dataTable = createDataTable( "dataTable", dataProvider );
        tableContainer.add( dataTable );
        dataTable.setItemsPerPage( criteria.limit ); // needs to be done BEFORE invoking setCurrentPage() as it resets the current page to zero
        dataTable.setCurrentPage( criteria.offset / criteria.limit );

        // navigation links
        final AjaxLink<Void> prevLink = new AjaxLink<>("prevLink")
        {
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                criteria.offset = Math.max( 0, criteria.offset - criteria.limit );
                target.add( tableContainer );
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisibilityAllowed( criteria.offset > 0 && criteria.limit > 0 );
            }
        };
        tableContainer.add( prevLink );

        final AjaxLink<Void> nextLink = new AjaxLink<>("nextLink")
        {
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                criteria.offset += criteria.limit;
                target.add( tableContainer );
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisibilityAllowed( criteria.offset > 0 && criteria.limit > 0 &&
                    (criteria.offset+criteria.limit < dataProvider.size()));
            }
        };
        tableContainer.add( nextLink );

        // interval picker
        final IModel<Interval> startDateModel =
            LambdaModel.of( () -> criteria.interval, x -> criteria.interval = x );

        final IntervalPicker startDatePicker = new IntervalPicker("intervalPicker",startDateModel);
        form.add( startDatePicker );

        // project choice
        final IModel<List<String>> projects = new LoadableDetachableModel<>()
        {
            @Override
            protected List<String> load()
            {
                final List<String> list = db.getDistinctProjects();
                list.sort(String.CASE_INSENSITIVE_ORDER);
                return list;
            }
        };

        final DropDownChoice<String> projectChoice =
            new DropDownChoice<>( "projectSelect",
                LambdaModel.of( () -> criteria.projectName, x -> criteria.projectName = x ),
                projects);
        if ( criteria.projectName == null && ! projects.getObject().isEmpty() ) {
            projectChoice.setModelObject(projects.getObject().get(0));
        }
        projectChoice.setRequired(true);
        form.add( projectChoice );

        // host choice
        final IModel<List<Host>> hosts  = new LoadableDetachableModel<>()
        {
            @Override
            protected List<Host> load()
            {
                final List<Host> result = db.getHosts();
                final Comparator<Host> comp = (a,b) -> {
                    if ( a.getHostName().isPresent() && b.getHostName().isPresent() ) {
                        return a.getHostName().get().compareToIgnoreCase(b.getHostName().get());
                    }
                    final String ip1 = a.getHostIP().getHostAddress();
                    final String ip2 = b.getHostIP().getHostAddress();
                    return ip1.compareToIgnoreCase(ip2);
                };
                result.sort( comp );
                result.add( 0, ANY_HOST );
                return result;
            }
        };

        final DropDownChoice<Host> hostsChoice =
            new DropDownChoice<>( "hostSelect",
                LambdaModel.of( () -> criteria.host, x -> criteria.host = x ),
                hosts,new ChoiceRenderer<>() {
                @Override
                public Object getDisplayValue(Host object)
                {
                    if ( object == ANY_HOST ) {
                        return "All Hosts";
                    }
                    if ( object.getHostName().isPresent() ) {
                        return object.getHostName().get() + " (" + object.getHostIP().getHostAddress() + ")";
                    }
                    return object.getHostIP().getHostAddress();
                }
            });
        hostsChoice.setRequired(true);
        form.add( hostsChoice );

        // branch choice
        final IModel<List<String>> branches  = new LoadableDetachableModel<>()
        {
            @Override
            protected List<String> load()
            {
                final List<String> branchNames = db.getBranchNames( criteria.projectName, criteria.host == ANY_HOST ? null : criteria.host );
                branchNames.add( 0, ANY_BRANCH );
                return branchNames;
            }
        };

        final DropDownChoice<String> branchChoice =
            new DropDownChoice<>( "branchSelect",
                LambdaModel.of( () -> criteria.branchName, x -> criteria.branchName = x ),
                branches,new ChoiceRenderer<>() {
                @Override
                public Object getDisplayValue(String object)
                {
                    //noinspection StringEquality
                    if ( object == ANY_BRANCH ) {
                        return "All Branches";
                    }
                    return object;
                }
            });
        branchChoice.setRequired(true);
        form.add( branchChoice );

        // apply button
        final Button applyButton = new AjaxButton( "applyButton" ) {
            @Override
            protected void onSubmit(AjaxRequestTarget target)
            {
                dataProvider.detach();
                target.add( tableContainer );
            }
        };
        form.add( applyButton );

        // compare button
        compareButton = new AjaxButton( "compareButton" ) {
            @Override
            protected void onSubmit(AjaxRequestTarget target)
            {
                final List<Build> builds = db.getBuilds( buildIdsForComparison );
                builds.sort( Comparator.comparing( Build::getStartTime ) );
                setResponsePage( new CompareByPhasesPage( builds, criteria ) );
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setEnabled( buildIdsForComparison.size() >= 2 );
            }
        };
        compareButton.setOutputMarkupId( true );
        compareButton.setDefaultFormProcessing( false );
        form.add( compareButton );
    }

    private MyDataTable<Build,TableColumn> createDataTable(String wicketId, ISortableDataProvider<Build,TableColumn> dataProvider)
    {
        final List<IColumn<Build, TableColumn>> columns = new ArrayList<>();

        columns.add( new AbstractColumn<>(new ResourceModel("column."+TableColumn.SELECTED_FOR_COMPARISON))
        {
            @Override
            public void populateItem(Item<ICellPopulator<Build>> cellItem, String componentId, IModel<Build> rowModel)
            {
                final Fragment frag = new Fragment( componentId, "checkboxFragment", HomePage.this );
                final IModel<Boolean> cbModel = new IModel<>() {

                    @Override
                    public Boolean getObject()
                    {
                        return buildIdsForComparison.contains( rowModel.getObject().id );
                    }

                    @Override
                    public void setObject(Boolean object)
                    {
                        if ( object ) {
                            buildIdsForComparison.add( rowModel.getObject().id );
                        } else {
                            buildIdsForComparison.remove( rowModel.getObject().id );
                        }
                    }
                };
                final AjaxCheckBox cb = new AjaxCheckBox( "checkbox", cbModel ) {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target)
                    {
                        target.add( compareButton );
                    }
                };
                frag.add( cb );
                cellItem.add( frag );
            }
        });

        columns.add( column( TableColumn.BUILD_TIMESTAMP, HomePage.this::getTimestamp ) );
        columns.add( column( TableColumn.PROJECT_NAME, build -> build.projectName ) );
        columns.add( column( TableColumn.BRANCH_NAME, build -> build.branchName , build -> "GIT hash: "+build.gitHash , 350 ) );
        columns.add( column( TableColumn.HOST_NAME, HomePage.this::getHostName ) );
        columns.add( column( TableColumn.BUILD_DURATION, build -> Utils.formatDuration( build.duration ) ) );
        return new MyDataTable<>( wicketId, columns, dataProvider,  criteria.limit) {
            @Override
            protected Item<Build> newRowItem(String id, int index, IModel<Build> model)
            {
                return new OddEvenItem<>( id, index, model );
            }
        };
    }

    private String getTimestamp(Build build) {
        DateTimeFormatter DF = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss" );
        return DF.format( build.startTime );
    }

    private String getHostName(Build build) {
        return build.host.getHostName().orElse(build.host.getHostIP().getHostAddress());
    }

    private IColumn<Build,TableColumn> column(TableColumn col, SerializableFunction<Build,?> func) {
        return column( col, func, null , -1 );
    }

    private void showModalPopup(Build build, AjaxRequestTarget target) {

        final Fragment dialogWithCloseButton = new Fragment( ModalDialog.CONTENT_ID, "buildInfoPanelFragment", HomePage.this ) {
            @Override
            protected void onInitialize()
            {
                super.onInitialize();
                final Form<Void> dummyForm = new Form<>( "dummyForm" );
                add( dummyForm );

                dummyForm.add( new BuildInfoPanel( "buildInfo", Model.of( build ) ) );
                final AjaxButton button = new AjaxButton( "closeButton" )
                {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target)
                    {
                        dialog.close( target );
                    }
                };
                button.setDefaultFormProcessing( false );
                dummyForm.add( button );
            }
        };

        dialog.setContent( dialogWithCloseButton );
        if ( dialog.isOpen() ) {
            target.add( dialog );
        } else {
            dialog.open( target );
        }
    }

    private IColumn<Build,TableColumn> column(TableColumn col, SerializableFunction<Build,?> func,SerializableFunction<Build,String> tooltipFunc, int tooltipWidthInPixels)
    {
        if ( tooltipFunc == null )
        {
            return new LambdaColumn<>( new ResourceModel( "column." + col.name() ), col, func ) {
                @Override
                public void populateItem(Item<ICellPopulator<Build>> item, String componentId, IModel<Build> rowModel)
                {
                    final LinkWithLabel<Build> label = new LinkWithLabel<>( componentId, getDataModel( rowModel ), rowModel ) {

                        @Override
                        protected void onClick(AjaxRequestTarget target, Build modelObject)
                        {
                            showModalPopup( modelObject, target );
                        }
                    };
                    item.add( label );
                }
            };
        }
        return new LambdaColumn<>( new ResourceModel( "column." + col.name() ), col, func ) {
            @Override
            public void populateItem(Item<ICellPopulator<Build>> item, String componentId, IModel<Build> rowModel)
            {
                final LinkWithLabel<Build> label = new LinkWithLabel<>( componentId, getDataModel( rowModel ), rowModel ) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, Build modelObject)
                    {
                        showModalPopup( modelObject, target );
                    }
                };
                label.getLabel().add( TooltipBehaviour.of( rowModel.map( tooltipFunc ) , tooltipWidthInPixels ) );
                item.add( label );
            }
        };
    }
}
