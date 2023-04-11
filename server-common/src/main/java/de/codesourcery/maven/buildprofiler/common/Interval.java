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
package de.codesourcery.maven.buildprofiler.common;

import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.ZonedDateTime;

public final class Interval implements Serializable
{
    public final ZonedDateTime start;
    public final ZonedDateTime end;

    public Interval(ZonedDateTime start, ZonedDateTime end)
    {
        Validate.notNull( start, "start must not be null" );
        Validate.notNull( end, "end must not be null" );
        Validate.isTrue( end.compareTo( start ) >= 0 , "start must be equal to/before end");
        this.start = start;
        this.end = end;
    }

    public java.sql.Timestamp startAsTimestamp() {
        return new Timestamp( start.toInstant().toEpochMilli() );
    }

    public java.sql.Timestamp endAsTimestamp() {
        return new Timestamp( end.toInstant().toEpochMilli() );
    }

    @Override
    public String toString()
    {
        return start + " - " + end;
    }

    public Duration getDuration() {
        return Duration.between( start, end );
    }
}
