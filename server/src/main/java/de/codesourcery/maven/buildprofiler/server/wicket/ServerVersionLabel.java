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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wicket.markup.html.basic.Label;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class ServerVersionLabel extends Label
{
    private static final Logger LOG = LogManager.getLogger( ServerVersionLabel.class );

    public ServerVersionLabel(String id)
    {
        super( id, ServerVersionLabel::getServerVersion );
    }

    private static final AtomicReference<String> serverVersion = new AtomicReference<>();

    private static String getServerVersion()
    {
        if ( serverVersion.get() == null ) {
            String s = "n/a";
            try ( InputStream in = ServerVersionLabel.class.getResourceAsStream( "/serverVersion.properties" ) ) {
                s = new String( in.readAllBytes(), StandardCharsets.UTF_8 );
            } catch(Exception ex) {
                LOG.error( "getServerVersion(): Failed", ex );
            }
            serverVersion.compareAndSet( null, s );
        }
        return serverVersion.get();
    }
}
