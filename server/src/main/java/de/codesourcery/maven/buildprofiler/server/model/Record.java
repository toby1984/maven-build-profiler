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
package de.codesourcery.maven.buildprofiler.server.model;

import de.codesourcery.maven.buildprofiler.server.LongInterval;
import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Record implements Serializable
{
    public long id;
    public long buildId;
    public long phaseId;
    public long pluginArtifactId;
    public String pluginVersion;
    public long artifactId;
    public String artifactVersion;
    public ZonedDateTime startTime;
    public ZonedDateTime endTime;

    // transient, calculated duration as result of Interval.merge() operation when grouping by phase/plugin/artifact
    private long durationMillis;

    public Duration duration() {
        if ( durationMillis != 0 ) {
            return Duration.ofMillis( durationMillis );
        }
        return Duration.ofMillis( endTime.toInstant().toEpochMilli() - startTime.toInstant().toEpochMilli() );
    }

    public void setTime(ZonedDateTime start, Duration interval)
    {
        setTime( start, start.plus( interval ) );
    }

    public void setTime(ZonedDateTime start, ZonedDateTime end) {
        Validate.isTrue( start.compareTo( end ) <= 0 );
        this.startTime = start;
        this.endTime = end;
    }

    public void setTime(long start, long end) {
        Validate.isTrue( start <= end );
        this.startTime = Instant.ofEpochMilli( start ).atZone( ZoneId.systemDefault() );
        this.endTime = Instant.ofEpochMilli( end ).atZone( ZoneId.systemDefault() );
    }

    public void setDurationMillis(long durationMillis)
    {
        Validate.isTrue( durationMillis >= 0 );
        this.durationMillis = durationMillis;
    }

    public long startTimeMillis() {
        return startTime.toInstant().toEpochMilli();
    }

    public long endTimeMillis() {
        return endTime.toInstant().toEpochMilli();
    }

    public static Optional<Duration> wallClockTime(List<Record> records) {
        Validate.notNull( records, "records must not be null" );
        return wallClockTimeForIntervals( records.stream().map( x -> LongInterval.of( x.startTimeMillis(), x.endTimeMillis() ) ).collect( Collectors.toList() ) );
    }

    static Optional<Duration> wallClockTimeForIntervals(List<LongInterval> intervals)
    {
        if ( intervals.isEmpty() ) {
            return Optional.empty();
        }
        final List<LongInterval> reduced = LongInterval.mergeIfPossible( intervals );

        final long millis = reduced.stream().mapToLong( LongInterval::length ).sum();
        return Optional.ofNullable( Duration.ofMillis( millis ) );
    }

    @Override
    public String toString()
    {
        return "duration: " + duration().toMillis() + " ms";
    }
}
