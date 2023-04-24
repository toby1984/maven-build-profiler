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
package de.codesourcery.maven.buildprofiler.server.wicket.components.charts;

import de.codesourcery.maven.buildprofiler.server.wicket.JsonResponseHandler;
import de.codesourcery.maven.buildprofiler.shared.Utils;
import org.apache.commons.lang3.StringUtils;
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

public abstract class PieChart extends Panel
{
    private static final Logger LOG = LogManager.getLogger( LineChart.class );

    private WebMarkupContainer c;
    private final IModel<List<PieChartItem>> dataset;

    public PieChart(String wicketId, IModel<List<PieChartItem>> dataset) {
        super( wicketId, dataset);
        Validate.notNull( dataset, "dataset must not be null" );
        this.dataset = dataset;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        c = new WebMarkupContainer( "piechart" );
        c.add( new AbstractDefaultAjaxBehavior()
        {
            @Override
            protected void onComponentTag(ComponentTag tag)
            {
                super.onComponentTag( tag );
                tag.getAttributes().put( "callbackUrl", getCallbackUrl() );
            }

            /*
let chart = new frappe.Chart("#chart", {
    data: {
      labels: ['Closeout','Construction','Procurement'],
      datasets: [{name: 'Projects per Phase', values: [3,18,16]}]
    },
    title: 'Projects per Phase',
	type: 'pie',
    colors: ['#f44336','#e91e63','#9c27b0'],
    height: 500,
    isNavigable: 0,
    valuesOverPoints: 1,
    barOptions: {
	  stacked: 0,
	},
    lineOptions: {
      dotSize: 4,
      hideLine: 0,
      hideDots: 0,
      heatline: 0,
      areaFill: 0,
    },
    axisOptions: {
	yAxisMode: 'span',
	xAxisMode: 'span',
	xIsSeries: 0,
	},
    maxLegendPoints: 20,
    maxSlices: 10,
    barOptions: { height: 20, depth: 2},
    discreteDomains: 1
  });
             */
            @Override
            protected void respond(AjaxRequestTarget target)
            {
                final StringBuilder json = new StringBuilder( "{ \"data\" : {" );

                // labels
                json.append("\"labels\" : [ ");
                final List<PieChartItem> items = dataset.getObject();
                for ( Iterator<PieChartItem> iterator = items.iterator(); iterator.hasNext(); )
                {
                    final PieChartItem item = iterator.next();
                    json.append( Utils.jsonString( item.label() ) );

                    if ( iterator.hasNext() ) {
                        json.append( "," );
                    }
                }
                json.append( "]," ); // end labels

                // datasets
                json.append( "\"datasets\" : [" );

                // dataset #1
                json.append( "{" );
                json.append( "\"name\" : " ).append( Utils.jsonString( PieChart.this.getChartLabel() ) ).append(",");

                // Y values
                json.append( "\"values\" : [ " );
                for ( Iterator<PieChartItem> iterator = items.iterator(); iterator.hasNext(); )
                {
                    final PieChartItem item = iterator.next();
                    json.append( item.value() );
                    if ( iterator.hasNext() ) {
                        json.append( "," );
                    }
                }
                // end values
                json.append( "] " );
                json.append( "}" );

                // end datasets
                json.append( "]" );
                json.append( "}, " ); // end data

                json.append( "\"title\" : %s ,".formatted( Utils.jsonString( getChartLabel() )) );
                json.append( "\"type\" : \"percentage\"," );
                json.append( "\"maxSlices\" : " ).append( items.size()+1 ).append( "," );
                json.append( "\"colors\" : [ ");

                for ( Iterator<PieChartItem> it = items.iterator(); it.hasNext(); )
                {
                    final PieChartItem item = it.next();
                    json.append( Utils.jsonString( toHtmlColor( item.color() ) ) );
                    if ( it.hasNext() ) {
                        json.append( ", " );
                    }
                }
                json.append( "]");

                json.append( "}" ); // end json

                if ( LOG.isDebugEnabled() ) {
                    LOG.debug( "linechart response:\n"+json );
                }
                JsonResponseHandler.respond( json );
            }
        } );
        c.setOutputMarkupPlaceholderTag( true );
        add( c );
    }

    protected static String toHtmlColor(Color awtColor) {
        return "#" + toHex( awtColor.getRed() ) + toHex( awtColor.getGreen() ) + toHex( awtColor.getBlue() );
    }

    private static String toHex(int value) {
        return StringUtils.leftPad( Integer.toHexString( value ), 2, '0' );
    }

    protected abstract String getChartLabel();

    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead( response );
        final String js = "charts.createChart('" + c.getMarkupId() + "');";
        response.render( OnDomReadyHeaderItem.forScript( js ) );
    }
}
