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
package de.codesourcery.maven.buildprofiler.server.db;

import de.codesourcery.maven.buildprofiler.server.model.Record;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DbServiceImplTest
{
    @Test
    public void testNoGrouping()
    {
        List<Record> list = List.of();
        DbServiceImpl.performGrouping( list, DbService.Grouping.NONE );
        assertThat( list ).isEmpty();

        list = List.of( new Record() );
        DbServiceImpl.performGrouping( list, DbService.Grouping.NONE );
        assertThat( list ).hasSize( 1 );

        list = new ArrayList<>();
        list.add( new Record() );
        list.add( new Record() );
        DbServiceImpl.performGrouping( list, DbService.Grouping.NONE );
        assertThat( list ).hasSize( 2 );
    }

    @Test
    public void testGroupByPhaseId()
    {
        List<Record> list = List.of();
        DbServiceImpl.performGrouping( list, DbService.Grouping.PHASE);
        assertThat( list ).isEmpty();

        list = List.of( new Record() );
        DbServiceImpl.performGrouping( list, DbService.Grouping.PHASE );
        assertThat( list ).hasSize( 1 );

        final ZonedDateTime now = ZonedDateTime.now();

        // merging not possible
        {
            final Record r1 = new Record();
            r1.phaseId = 1;
            r1.setTime( now, Duration.ofMillis( 5 ) );
            final Record r2 = new Record();
            r2.phaseId = 2;
            r2.setTime( now, Duration.ofMillis( 15 ) );

            list = new ArrayList<>( List.of( r1, r2 ) );

            DbServiceImpl.performGrouping( list, DbService.Grouping.PHASE );
            assertThat( list ).hasSize( 2 );
            assertThat( list ).anyMatch( x -> x.phaseId == 1 );
            assertThat( list ).anyMatch( x -> x.phaseId == 2 );
        }

        // merging possible
        {
            final Record r1 = new Record();
            r1.setTime( now , Duration.ofMillis( 15 ) );
            r1.phaseId = 1;

            final Record r2 = new Record();
            r2.phaseId = 2;
            r2.setTime(now , Duration.ofMillis( 35 ) );

            final Record r3 = new Record();
            r3.phaseId = 1;
            r3.setTime( now.plusMinutes( 1 ), Duration.ofMillis( 5 ) );

            list = new ArrayList<>( List.of( r1, r2, r3) );

            DbServiceImpl.performGrouping( list, DbService.Grouping.PHASE );
            assertThat( list ).hasSize( 2 );
            assertThat( list ).anyMatch( x -> x.phaseId == 1 );
            assertThat( list ).anyMatch( x -> x.phaseId == 2 );

            assertThat( list.stream().filter( x -> x.phaseId == 1 ).findFirst().get().duration() ).isEqualTo( Duration.ofMillis( 20 ) );
        }
    }

    @Test
    public void testGroupByArtifactId()
    {
        List<Record> list = List.of();
        DbServiceImpl.performGrouping( list, DbService.Grouping.ARTIFACT);
        assertThat( list ).isEmpty();

        list = List.of( new Record() );
        DbServiceImpl.performGrouping( list, DbService.Grouping.ARTIFACT );
        assertThat( list ).hasSize( 1 );

        final ZonedDateTime now = ZonedDateTime.now();

        // merging not possible
        {
            final Record r1 = new Record();
            r1.artifactId = 1;
            r1.setTime( now, Duration.ofMillis( 5 ) );
            final Record r2 = new Record();
            r2.artifactId = 2;
            r2.setTime( now, Duration.ofMillis( 15 ) );

            list = new ArrayList<>( List.of( r1, r2 ) );

            DbServiceImpl.performGrouping( list, DbService.Grouping.ARTIFACT );
            assertThat( list ).hasSize( 2 );
            assertThat( list ).anyMatch( x -> x.artifactId == 1 );
            assertThat( list ).anyMatch( x -> x.artifactId == 2 );
        }

        // merging possible
        {
            final Record r1 = new Record();
            r1.setTime( now , Duration.ofMillis( 15 ) );
            r1.artifactId = 1;

            final Record r2 = new Record();
            r2.artifactId = 2;
            r2.setTime(now , Duration.ofMillis( 35 ) );

            final Record r3 = new Record();
            r3.artifactId = 1;
            r3.setTime( now.plusMinutes( 1 ), Duration.ofMillis( 5 ) );

            list = new ArrayList<>( List.of( r1, r2, r3) );

            DbServiceImpl.performGrouping( list, DbService.Grouping.ARTIFACT );
            assertThat( list ).hasSize( 2 );
            assertThat( list ).anyMatch( x -> x.artifactId == 1 );
            assertThat( list ).anyMatch( x -> x.artifactId == 2 );

            assertThat( list.stream().filter( x -> x.artifactId == 1 ).findFirst().get().duration() ).isEqualTo( Duration.ofMillis( 20 ) );
        }
    }

    @Test
    public void testGroupByPluginArtifactId()
    {
        List<Record> list = List.of();
        DbServiceImpl.performGrouping( list, DbService.Grouping.PLUGIN);
        assertThat( list ).isEmpty();

        list = List.of( new Record() );
        DbServiceImpl.performGrouping( list, DbService.Grouping.PLUGIN );
        assertThat( list ).hasSize( 1 );

        final ZonedDateTime now = ZonedDateTime.now();

        // merging not possible
        {
            final Record r1 = new Record();
            r1.pluginArtifactId = 1;
            r1.setTime( now, Duration.ofMillis( 5 ) );
            final Record r2 = new Record();
            r2.pluginArtifactId = 2;
            r2.setTime( now, Duration.ofMillis( 15 ) );

            list = new ArrayList<>( List.of( r1, r2 ) );

            DbServiceImpl.performGrouping( list, DbService.Grouping.PLUGIN );
            assertThat( list ).hasSize( 2 );
            assertThat( list ).anyMatch( x -> x.pluginArtifactId == 1 );
            assertThat( list ).anyMatch( x -> x.pluginArtifactId == 2 );
        }

        // merging possible
        {
            final Record r1 = new Record();
            r1.setTime( now , Duration.ofMillis( 15 ) );
            r1.pluginArtifactId = 1;

            final Record r2 = new Record();
            r2.pluginArtifactId = 2;
            r2.setTime(now , Duration.ofMillis( 35 ) );

            final Record r3 = new Record();
            r3.pluginArtifactId = 1;
            r3.setTime( now.plusMinutes( 1 ), Duration.ofMillis( 5 ) );

            list = new ArrayList<>( List.of( r1, r2, r3) );

            DbServiceImpl.performGrouping( list, DbService.Grouping.PLUGIN );
            assertThat( list ).hasSize( 2 );
            assertThat( list ).anyMatch( x -> x.pluginArtifactId == 1 );
            assertThat( list ).anyMatch( x -> x.pluginArtifactId == 2 );

            assertThat( list.stream().filter( x -> x.pluginArtifactId == 1 ).findFirst().get().duration() ).isEqualTo( Duration.ofMillis( 20 ) );
        }
    }
}