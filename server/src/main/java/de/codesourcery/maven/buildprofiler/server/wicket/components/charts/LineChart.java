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

import java.util.Iterator;
import java.util.List;

public abstract class LineChart<T extends XYDataItem> extends Panel
{
    private static final Logger LOG = LogManager.getLogger( LineChart.class );

    private WebMarkupContainer c;
    private final IModel<DataSet<T>> dataset;

    public LineChart(String wicketId, IModel<DataSet<T>> dataset) {
        super( wicketId, dataset);
        Validate.notNull( dataset, "dataset must not be null" );
        this.dataset = dataset;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        c = new WebMarkupContainer( "linechart" );
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
                final StringBuilder json = new StringBuilder( "{ \"data\" : {" );

                // labels
                json.append("\"labels\" : [ ");
                final List<T> items = dataset.getObject().getItems();
                for ( Iterator<T> iterator = items.iterator(); iterator.hasNext(); )
                {
                    final T item = iterator.next();
                    final String s = LineChart.this.getXAxisLabelFor( item );
                    json.append( Utils.jsonString( s ) );

                    if ( iterator.hasNext() ) {
                        json.append( "," );
                    }
                }
                json.append( "]," ); // end labels

                // datasets
                json.append( "\"datasets\" : [" );

                // dataset #1
                json.append( "{" );
                json.append( "\"name\" : " ).append( Utils.jsonString( LineChart.this.getChartLabel() ) ).append(",");

                // Y values
                json.append( "\"values\" : [ " );
                for ( Iterator<T> iterator = items.iterator(); iterator.hasNext(); )
                {
                    final T item = iterator.next();
                    json.append( item.getY() );
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
                json.append( "\"type\" : \"line\"," );
                json.append( "\"colors\" : [ \"black\" ] " );

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

    protected abstract String getXAxisLabelFor(T x);

    protected abstract String getChartLabel();

    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead( response );
        final String js = "charts.linechart('" + c.getMarkupId() + "');";
        response.render( OnDomReadyHeaderItem.forScript( js ) );
    }
}
