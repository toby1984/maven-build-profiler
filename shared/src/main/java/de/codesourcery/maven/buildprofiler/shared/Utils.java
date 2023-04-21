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
package de.codesourcery.maven.buildprofiler.shared;

public class Utils
{
    public static String jsonString(String input)
    {
        if ( input == null )
        {
            return "null";
        }
        final char[] array = input.toCharArray();
        final StringBuilder result = new StringBuilder(array.length);
        for ( char c : array )
        {
            switch( c )
            {
                case '\\' -> result.append( "\\\\" );
                case '"' -> result.append( "\\\"" );
                case '\f' -> result.append( "\\f" );
                case '\b' -> result.append( "\\b" );
                case '\t' -> result.append( "\\t" );
                case '\r' -> result.append( "\\r" );
                case '\n' -> result.append( "\\n" );
                default -> result.append( c );
            }
        }
        return '"' + result.toString() + '"';
    }
}
