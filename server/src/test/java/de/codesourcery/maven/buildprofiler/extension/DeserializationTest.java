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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DeserializationTest
{
    @Test
    void test() throws JsonProcessingException
    {
        final MyExtension.ArtifactCoords artifact1 = new MyExtension.ArtifactCoords("a1-group", "a1-artifact", "1.0-a1-SNAPSHOT");
        final MyExtension.ArtifactCoords plugin1= new MyExtension.ArtifactCoords("p1-group", "p1-artifact", "1.0-p1-SNAPSHOT");
        final MyExtension.ArtifactCoords plugin2= new MyExtension.ArtifactCoords("p2-group", "p2-artifact", "1.0-p2-SNAPSHOT");

        final List<MyExtension.ExecutionRecord> list = List.of(
            new MyExtension.ExecutionRecord( artifact1, plugin1, "clean", 23 ),
            new MyExtension.ExecutionRecord( artifact1,plugin2, "process-sources", 25 ),
            new MyExtension.ExecutionRecord( artifact1,plugin2, "process-sources", 25 )
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
        final BuildResult.Record record = value.records.get(0);
        assertThat( record.artifactId ).isEqualTo( artifact1.artifactId() );
        assertThat( record.groupId ).isEqualTo( artifact1.groupId());
        assertThat( record.version ).isEqualTo( artifact1.version() );

        assertThat( record.executionTimesByPlugin ).containsEntry( plugin1.getAsString(), Map.of("clean", 23 ) );
        assertThat( record.executionTimesByPlugin ).containsEntry( plugin2.getAsString(), Map.of("process-sources", 50 ) );
    }
}
