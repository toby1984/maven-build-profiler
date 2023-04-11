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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class XYLineChart<A,B> extends AbstractDefaultAjaxBehavior
{
    private static final Logger LOG = LogManager.getLogger( XYLineChart.class );

    private static final JavaScriptResourceReference MY_JS =
        new JavaScriptResourceReference( XYLineChart.class, "linechart.js" );

    private static final JavaScriptResourceReference D3_JS =
        new JavaScriptResourceReference( XYLineChart.class, "d3_7.8.4.min.js" );

    private final List<XYDataPoint<A,B>> data;
    private final IXYTransformer<A,B> transformer;

    public XYLineChart(List<XYDataPoint<A, B>> data, IXYTransformer<A, B> transformer)
    {
        Validate.notNull(data, "data must not be null");
        Validate.notNull(transformer, "transformer must not be null");
        this.data = data;
        this.transformer = transformer;
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response)
    {
        super.renderHead(component, response);
        response.render(JavaScriptReferenceHeaderItem.forReference(D3_JS));
        response.render(JavaScriptReferenceHeaderItem.forReference(MY_JS));
    }

    // Returns Javascript function to use for transforming
    // CSV X value on the client-side
    protected String getCsvXValueTransformerFunction() {
        return "x => x";
    }

    // Returns Javascript function to use for transforming
    // CSV Y value on the client-side
    protected String getCsvYValueTransformerFunction() {
        return "y => y";
    }

    @Override
    protected void respond(AjaxRequestTarget ajaxRequestTarget)
    {
        final String requestType = this.getComponent().getRequest().getRequestParameters().getParameterValue("requestType").toOptionalString();
        final IRequestHandler handler;
        if ( "chartDescriptor".equals(requestType) ) {
            handler = requestCycle ->
            {
                final WebResponse r = (WebResponse)requestCycle.getResponse();
                r.setContentType("application/json; charset=UTF-8");
                r.disableCaching();
                try
                {
                    final Map<String,String> response = new HashMap<>();
                    response.put("xValueTransformerFunction", "");
                    response.put("yValueTransformerFunction", "");
                    final String json = new ObjectMapper().writeValueAsString( response );
                    r.getOutputStream().write( json.getBytes( StandardCharsets.UTF_8 ) );
                }
                catch( IOException e )
                {
                    throw new RuntimeException( e );
                }
            };
        }
        else if ( "csvData".equals(requestType) )
        {
            handler = requestCycle ->
            {
                final WebResponse r = (WebResponse) requestCycle.getResponse();
                r.setContentType("text/csv; charset=UTF-8");
                r.disableCaching();
                try
                {
                    final StringBuilder csv = new StringBuilder();
                    csv.append("x;y\n");

                    for (Iterator<XYDataPoint<A, B>> iterator = data.iterator(); iterator.hasNext(); )
                    {
                        final XYDataPoint<A, B> datum = iterator.next();
                        final Number x = transformer.getX(datum);
                        final Number y = transformer.getY(datum);
                        csv.append(x).append(";").append(y);
                        if (iterator.hasNext())
                        {
                            csv.append("\n");
                        }
                    }
                    r.getOutputStream().write(csv.toString().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            };
        } else {
            LOG.error("Unknown/missing 'requestType' parameter value: '" + requestType + "'");
            return;
        }
        RequestCycle.get().scheduleRequestHandlerAfterCurrent(handler);
    }
}
