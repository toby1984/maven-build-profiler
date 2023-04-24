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
package de.codesourcery.maven.buildprofiler.server;

import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class LongInterval
{
    private long start;
    private long end;

    public LongInterval(long start, long end)
    {
        Validate.isTrue( end >= start );
        this.start = start;
        this.end = end;
    }

    public long length() {
        return end - start;
    }

    @Override
    public boolean equals(Object obj)
    {
        if ( obj instanceof LongInterval interval)
        {
            return start == interval.start && end == interval.end;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( start, end );
    }

    public static LongInterval of(long start, long end) {
        return new LongInterval( start, end );
    }

    public boolean mergeInPlace(LongInterval other) {
        return mergeInPlace( other.start, other.end );
    }

    public boolean mergeInPlace(long oStart, long oEnd )
    {
        if ( canBeMergedWith( oStart, oEnd ) ) {
            this.start = Math.min( this.start, oStart );
            this.end = Math.max( this.end, oEnd);
            return true;
        }
        return false;
    }

    public Optional<LongInterval> merge(LongInterval other) {
        return merge( this, other );
    }

    public static Optional<LongInterval> merge(LongInterval a, LongInterval b)
    {
        if ( a.canBeMergedWith( b ) )
        {
            return Optional.of( new LongInterval( Math.min( a.start, b.start ), Math.max( a.end, b.end ) ) );
        }
        return Optional.empty();
    }

    public boolean contains(long v) {
        return start <= v && v < end;
    }

    public boolean canBeMergedWith(LongInterval other) {
        return canBeMergedWith( other.start, other.end );
    }

    public boolean canBeMergedWith(long oStart, long oEnd)
    {
        if ( this.start <= oStart && oStart < this.end )
        {
            return true;
        }
        if ( this.start < oEnd && oEnd < this.end )
        {
            return true;
        }
        return oStart <= start && oEnd >= this.end;
    }

    public long getStart()
    {
        return start;
    }

    public long getEnd()
    {
        return end;
    }

    @Override
    public String toString()
    {
        return "Interval[ " +
            start + " - " + end + ", len "+length()+" ]";
    }

    public static List<LongInterval> mergeIfPossible(List<LongInterval> intervals) {
        Validate.notNull( intervals, "intervals must not be null" );
        final List<LongInterval> result = new ArrayList<>();
        if ( intervals.size() > 1 )
        {
            final Iterator<LongInterval> it = intervals.iterator();
            result.add( it.next() );

outer:
            while (it.hasNext())
            {
                final LongInterval iv = it.next();
                for ( LongInterval candidate : result )
                {
                    if ( candidate.canBeMergedWith( iv ) ) {
                        candidate.mergeInPlace( iv );
                        continue outer;
                    }
                }
                result.add( iv );
            }
        } else {
            result.addAll( intervals );
        }
        return result;
    }
}
