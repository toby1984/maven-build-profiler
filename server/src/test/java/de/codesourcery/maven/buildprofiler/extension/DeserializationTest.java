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
package de.codesourcery.maven.buildprofiler.extension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.codesourcery.maven.buildprofiler.common.BuildResult;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DeserializationTest
{
    @Test
    void test() throws JsonProcessingException
    {
        final List<MyExtension.ExecutionRecord> list = List.of(
            new MyExtension.ExecutionRecord( "group", "artifact", "1.0-SNAPSHOT", "clean", 23 ),
            new MyExtension.ExecutionRecord( "group", "artifact", "1.0-SNAPSHOT", "compile", 25 )
        );
        final MyExtension instance = new MyExtension();
        instance.gitHash = "deadbeef";
        instance.maxConcurrency.set(123);
        instance.projectName = "project";
        instance.branchName = "branch";

        MyExtension.profilingStartTime = System.currentTimeMillis() - 1000;
        MyExtension.mojoStartTime.set(System.currentTimeMillis() - 1000);

        final String json = MyExtension.getJSONRequest( list, instance );

        System.out.println(json);

        final ObjectMapper mapper = new ObjectMapper();
        final BuildResult value = mapper.readValue( json, BuildResult.class );

        assertThat( value.buildStartTime ).isPositive();
        assertThat( value.buildDurationMillis ).isCloseTo( 1000L, Offset.offset( 10L) );
        assertThat( value.hostName ).isNotBlank();
        assertThat( value.maxConcurrency ).isEqualTo( 123 );
        assertThat( value.jvmVersion).isNotBlank();
        assertThat( value.availableProcessors).isPositive();
        assertThat( value.projectName ).isEqualTo( "project" );
        assertThat( value.branchName ).isEqualTo( "branch" );
        assertThat( value.gitHash).isNotBlank();
        assertThat( value.systemProperties ).isNotEmpty();
        assertThat( value.records ).hasSize( 1 );
        assertThat( value.records.get( 0 ).artifactId ).isEqualTo( "artifact" );
        assertThat( value.records.get( 0 ).groupId ).isEqualTo( "group" );
        assertThat( value.records.get( 0 ).version ).isEqualTo( "1.0-SNAPSHOT" );
        assertThat( value.records.get( 0 ).executionTimesByPhase ).isNotEmpty();
        assertThat( value.records.get( 0 ).executionTimesByPhase ).containsEntry( "clean", 23 );
        assertThat( value.records.get( 0 ).executionTimesByPhase ).containsEntry( "compile", 25 );
    }
}
