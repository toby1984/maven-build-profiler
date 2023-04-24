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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MyExtensionTest
{
    @Test
    void testJsonGeneration()
    {
        final MyExtension.ArtifactCoords artifact = coords("a-group", "a-artifact", "1.0-a-SNAPSHOT");
        final MyExtension.ArtifactCoords plugin1= coords("p1-group", "p1-artifact", "1.0-p1-SNAPSHOT");
        final MyExtension.ArtifactCoords plugin2= coords("p2-group", "p2-artifact", "1.0-p2-SNAPSHOT");

        long now = 1682321652229L;

        final List<MyExtension.ExecutionRecord> list = List.of(
            new MyExtension.ExecutionRecord( artifact, plugin1, "clean", now, now + 10 ),
            new MyExtension.ExecutionRecord( artifact, plugin2, "compile", now + 15, now + 20 )
        );
        final MyExtension instance = new MyExtension();
        instance.gitHash = "deadbeef";
        instance.maxConcurrency.set(123);
        MyExtension.startupTimestamp = now - 1000;
        final String json = MyExtension.getJSONRequest( list, instance , now );
        System.out.println(json);
    }

    private static MyExtension.ArtifactCoords coords(String groupId, String artifactId, String version) {
        return new MyExtension.ArtifactCoords(groupId, artifactId, version);
    }
}