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

import de.codesourcery.maven.buildprofiler.server.db.DbService;
import de.codesourcery.maven.buildprofiler.server.model.Artifact;
import de.codesourcery.maven.buildprofiler.server.model.Build;
import de.codesourcery.maven.buildprofiler.server.model.LifecyclePhase;
import de.codesourcery.maven.buildprofiler.server.model.Record;
import de.codesourcery.maven.buildprofiler.server.wicket.components.charts.PieChart;
import de.codesourcery.maven.buildprofiler.server.wicket.components.charts.PieChartItem;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BuildInfoPanel extends Panel implements IWicketUtils
{
    private final IModel<Build> model;

    @SpringBean
    private DbService dbService;

    public BuildInfoPanel(String id, IModel<Build> model)
    {
        super( id );
        Validate.notNull( model, "model must not be null" );
        this.model = model;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        final IModel<List<Record>> recordsModel = new LoadableDetachableModel<>()
        {
            @Override
            protected List<Record> load()
            {
                return dbService.getRecords( model.getObject().id );
            }
        };

        // label
        add( new Label( "projectName", model.map(x->x.projectName)) );
        add( new Label( "branchName", model.map( x -> x.branchName ) ) );
        add( new Label( "hostName", model.map( x -> x.host.getUIString() ) ) );

        // by-phase pie chart
        final IModel<List<PieChartItem>> byPhaseModel = recordsModel.map( list -> {

            // group records by Phase
            final Map<Long, List<Record>> byPhaseID = list.stream().collect( Collectors.groupingBy( x -> x.phaseId ) );
            final Map<Long, LifecyclePhase> phasesByID = dbService.getPhasesByIDs( byPhaseID.keySet() ).stream().collect( Collectors.toMap( x -> x.phaseId, y -> y ) );

            final Iterator<Color> colorIterator = ColorUtils.getChartColorSupplier();
            final List<PieChartItem> result = new ArrayList<>();

            final long totalDuration = list.stream().mapToLong( x -> x.duration.toMillis() ).sum();

            byPhaseID.forEach( (phaseId, records) -> {
                final long time = records.stream().mapToLong( x -> x.duration.toMillis() ).sum();
                final double percDuration = 100.0 * ( time / (double) totalDuration);
                final LifecyclePhase phase = phasesByID.get( phaseId );
                result.add( new PieChartItem( phase.name, colorIterator.next(), percDuration ) );
            });
            return result;
        });

        add( new PieChart("timeByPhase",byPhaseModel)
        {
            @Override
            protected String getChartLabel()
            {
                return "Time By Phase";
            }
        } );

        // by plugin pie chart
        final IModel<List<PieChartItem>> byPluginModel = recordsModel.map( list -> {

            // group records by plugin artifact id
            final Map<Long, List<Record>> byPluginArtifactId = list.stream().collect( Collectors.groupingBy( x -> x.pluginArtifactId ) );

            final Map<Long, Artifact> pluginsByArtifactId = dbService.getArtifactsByIDs( byPluginArtifactId.keySet() ).stream().collect(
                Collectors.toMap( x->x.id, y -> y ) );

            final long totalDuration = list.stream().mapToLong( x -> x.duration.toMillis() ).sum();

            final Iterator<Color> colorIterator = ColorUtils.getChartColorSupplier();
            final List<PieChartItem> result = new ArrayList<>();
            byPluginArtifactId.forEach( (pluginArtifactId, records) -> {
                final long time = records.stream().mapToLong( x -> x.duration.toMillis() ).sum();
                final double percDuration = 100.0 * (time / (double) totalDuration);
                final Record r = records.get( 0 );
                final Artifact plugin = pluginsByArtifactId.get( r.pluginArtifactId );
                final String label;
                if ( "org.apache.maven.plugins".equals( plugin.groupId ) ) {
                    label = plugin.artifactId + ":" + r.pluginVersion;
                } else {
                    label = plugin.toUIString( r.pluginVersion );
                }
                result.add( new PieChartItem( label, colorIterator.next(), percDuration ) );
            } );
            return result;
        });

        add( new PieChart("timeByPlugin", byPluginModel)
        {
            @Override
            protected String getChartLabel()
            {
                return "Time By Plugin";
            }
        } );

        // by artifact pie chart
        final IModel<List<PieChartItem>> byArtifactModel = recordsModel.map( list -> {

            // group records by artifact id
            final Map<Long, List<Record>> byArtifactId =
                list.stream().collect( Collectors.groupingBy( x -> x.artifactId ) );

            final Map<Long, Artifact> artifactsById =
                dbService.getArtifactsByIDs( byArtifactId.keySet() ).stream().collect(
                Collectors.toMap( x->x.id, y -> y ) );

            final long totalDuration = list.stream().mapToLong( x -> x.duration.toMillis() ).sum();

            final Iterator<Color> colorIterator = ColorUtils.getChartColorSupplier();
            final List<PieChartItem> result = new ArrayList<>();
            byArtifactId.forEach( (artifactId, records ) -> {
                final long time = records.stream().mapToLong( x -> x.duration.toMillis() ).sum();
                final double percDuration = 100.0 * ( time / (double) totalDuration);
                final Artifact a = artifactsById.get( artifactId );
                final String label = a.toUIString( records.get( 0 ).artifactVersion );
                result.add( new PieChartItem( label, colorIterator.next(), percDuration ) );
            } );
            return result;
        });

        add( new PieChart("timeByArtifact", byArtifactModel)
        {
            @Override
            protected String getChartLabel()
            {
                return "Time By Artifact";
            }
        } );

    }
}
