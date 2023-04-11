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

import java.time.Duration;

public class Utils
{
    public static String formatDuration(Duration d) {

        long millis = d.toMillis();
        StringBuilder result = new StringBuilder();
        if ( millis >= 60 * 60 * 1000 ) {
            int hours = (int) (millis / (60 * 60 * 1000));
            result.append( hours );
            millis -= (hours * 60 * 60 * 1000L);
        }
        if ( millis >= 60 * 1000 ) {
            int minutes = (int) (millis / (60 * 1000));
            if ( ! result.isEmpty() ) {
                result.append( ":" );
            }
            result.append( minutes );
            millis -= (minutes* 60 * 1000L);
        }
        if ( millis >= 1000 ) {
            int seconds = (int) (millis / 1000);
            if ( ! result.isEmpty() ) {
                result.append( ":" );
            }
            result.append( seconds );
            millis -= (seconds * 1000L);
        }
        if ( millis > 0 ) {
            if ( ! result.isEmpty() ) {
                result.append( "." );
            }
            result.append( millis );
        }
        return result.toString();
    }
}
