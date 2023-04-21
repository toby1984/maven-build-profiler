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

import org.apache.commons.lang3.Validate;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class JsonResponseHandler implements IRequestHandler
{
    private final CharSequence json;

    private JsonResponseHandler(CharSequence json)
    {
        Validate.notBlank( json, "json must not be null or blank" );
        this.json = json;
    }

    public static void respond(CharSequence json)
    {
        // RequestCycle.get().scheduleRequestHandlerAfterCurrent( new JsonResponseHandler( json ) );
        final RequestCycle cycle = RequestCycle.get();
        cycle.scheduleRequestHandlerAfterCurrent( null );
        send( (WebResponse) cycle.getResponse(), json );
    }

    private static void send(WebResponse r, CharSequence json) {
        r.setContentType("application/json; charset=UTF-8");
        r.disableCaching();
        try
        {
            r.getOutputStream().write( json.toString().getBytes( StandardCharsets.UTF_8 ) );
        }
        catch( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void respond(IRequestCycle requestCycle)
    {
        final WebResponse r = (WebResponse) requestCycle.getResponse();
        send(r, json );
    }
}