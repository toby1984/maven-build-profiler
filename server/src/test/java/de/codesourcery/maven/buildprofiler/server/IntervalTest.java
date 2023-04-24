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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntervalTest
{

    @Test
    public void testMergeIfPossible() {

        {
            final List<LongInterval> result = LongInterval.mergeIfPossible( List.of( iv( 1, 2 ) ) );
            assertThat( result ).hasSize( 1 );
            assertThat( result ).contains( iv( 1, 2 ) );
        }

        {
            final List<LongInterval> result = LongInterval.mergeIfPossible( List.of( iv( 1, 2 ), iv( 3, 4 ) ) );
            assertThat( result ).hasSize( 2 );
            assertThat( result ).contains( iv( 1, 2 ) );
            assertThat( result ).contains( iv( 3, 4 ) );
        }

        {
            final List<LongInterval> result = LongInterval.mergeIfPossible( List.of( iv( 1, 3 ), iv( 2, 4 ), iv (5,6) ) );
            assertThat( result ).hasSize( 2 );
            assertThat( result ).contains( iv( 1, 4 ) );
            assertThat( result ).contains( iv( 5, 6 ) );
        }
    }

    @Test
    public void testMerge()
    {
        assertThat( iv( 5, 7 ).merge( iv( 4, 8 ) ) ).hasValue( iv( 4, 8 ) );
        assertThat( iv( 4, 8 ).merge( iv( 5, 7 ) ) ).hasValue( iv( 4, 8 ) );
        assertThat( iv( 1, 2 ).merge( iv( 1, 3 ) ) ).hasValue( iv( 1, 3 ) );
        assertThat( iv( 1, 2 ).merge( iv( 3, 4 ) ) ).isEmpty();
        assertThat( iv( 3, 4 ).merge( iv( 1, 2 ) ) ).isEmpty();
        assertThat( iv( 1, 5 ).merge( iv( 3, 4 ) ) ).hasValue( iv( 1, 5 ) );
        assertThat( iv( 3, 4 ).merge( iv( 1, 5 ) ) ).hasValue( iv( 1, 5 ) );
    }

    @Test
    public void test() {
        assertCanNotBeMerged( iv( 5, 7 ), iv( 7, 8 ) );

        assertThat( iv( 5,6 ).contains( 5 ) ).isTrue();
        assertThat( iv( 5,6 ).contains( 5 ) ).isTrue();
        assertThat( iv( 5,6 ).contains( 6 ) ).isFalse();
        assertThat( iv( 5,6 ).contains( 4 ) ).isFalse();
        assertCanBeMerged( iv( 5, 7 ), iv( 5, 7 ) );
        assertCanBeMerged( iv( 3, 4 ), iv( 2, 5 ) );

        assertCanNotBeMerged( iv( 5, 7 ), iv( 4, 5 ) );
    }

    private void assertCanBeMerged(LongInterval a, LongInterval b)
    {
        assertThat( a.canBeMergedWith( b ) ).isTrue();
        assertThat( b.canBeMergedWith( a ) ).isTrue();
    }

    private void assertCanNotBeMerged(LongInterval a, LongInterval b)
    {
        assertThat( a.canBeMergedWith( b ) ).isFalse();
        assertThat( b.canBeMergedWith( a ) ).isFalse();
    }

    private static LongInterval iv(long a, long b) {
        return LongInterval.of( a, b );
    }
}