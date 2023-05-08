package de.codesourcery.maven.buildprofiler.server.wicket.components.charts;

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

import de.codesourcery.maven.buildprofiler.server.wicket.JsonResponseHandler;
import de.codesourcery.maven.buildprofiler.server.wicket.ServerUtils;
import de.codesourcery.maven.buildprofiler.shared.SharedUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BarChart extends Panel
{
    private static final Logger LOG = LogManager.getLogger( LineChart.class );

    private WebMarkupContainer c;
    private final IModel<BarChartData> model;

    public BarChart(String wicketId, IModel<BarChartData> model) {
        super( wicketId, model );
        Validate.notNull( model, "dataset must not be null" );
        this.model = model;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        c = new WebMarkupContainer( "barchart" );
        c.add( new AbstractDefaultAjaxBehavior()
        {
            @Override
            protected void onComponentTag(ComponentTag tag)
            {
                super.onComponentTag( tag );
                tag.getAttributes().put( "callbackUrl", getCallbackUrl() );
            }

            @Override
            protected void respond(AjaxRequestTarget target)
            {
                final String json = generateJSON( model.getObject(), getChartLabel() );
                if ( LOG.isDebugEnabled() ) {
                    LOG.debug( "bar response:\n"+json );
                }
                JsonResponseHandler.respond( json );
            }
        } );
        c.setOutputMarkupPlaceholderTag( true );
        add( c );
    }

    private static String generateJSON(BarChartData data, String chartLabel)
    {
        final StringBuilder json = new StringBuilder( "{ \"data\" : {" );

        // labels
        json.append("\"labels\" : [ ");
        final List<String> labels = data.labels;
        json.append( labels.stream().map( SharedUtils::jsonString ).collect( Collectors.joining(",") ) );
        json.append( "]," ); // end labels

        // datasets
        json.append( "\"datasets\" : [" );

        for ( Iterator<BarChartData.DataSet> iter = data.datasets.iterator(); iter.hasNext(); )
        {
            final BarChartData.DataSet ds = iter.next();
            json.append( "{" );
            json.append( "\"name\" : " ).append( SharedUtils.jsonString( ds.name() ) ).append( "," );

            // Y values
            json.append( "\"values\" : [ " );
            for ( Iterator<Double> iterator = ds.data().iterator(); iterator.hasNext(); )
            {
                json.append( iterator.next() );
                if ( iterator.hasNext() )
                {
                    json.append( "," );
                }
            }
            json.append( "] " );
            // end values

            json.append( "}" );

            if ( iter.hasNext() ) {
                json.append( ", " );
            }
        }

        // end datasets
        json.append( "]" );
        json.append( "}, " );

        json.append( "\"title\" : %s ,".formatted( SharedUtils.jsonString( chartLabel )) );
        json.append( "\"type\" : \"bar\"," );
        final String colors = data.datasets.stream().map( x -> ServerUtils.toHtmlColor( x.color() ) )
            .map( SharedUtils::jsonString )
            .collect( Collectors.joining( "," ) );
        json.append( "\"colors\" : [ " ).append( colors ).append( " ] " );

        json.append( "}" ); // end json
        return json.toString();
    }

    protected abstract String getChartLabel();

    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead( response );
        final String js = "charts.createChart('" + c.getMarkupId() + "');";
        response.render( OnDomReadyHeaderItem.forScript( js ) );
    }

    public static void main(String[] args)
    {
        final BarChartData data = new BarChartData();
        data.addLabels( "label1", "label2" , "label3" );
        data.add( new BarChartData.DataSet( "artifact #1", Color.RED, List.of( 1d, 2d, 3d ) ) );
        data.add( new BarChartData.DataSet( "artifact #2", Color.GREEN, List.of( 4d, 5d, 6d ) ) );
        System.out.println( generateJSON( data, "chart label" ) );
    }
}
