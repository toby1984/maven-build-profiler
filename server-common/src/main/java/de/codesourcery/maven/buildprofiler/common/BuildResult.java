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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.codesourcery.maven.buildprofiler.shared.ArtifactCoords;

import java.util.List;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class BuildResult
{
    @JsonProperty(required = true)
    public int jsonSyntaxVersion;

    @JsonProperty(required = true)
    public long buildStartTime;

    @JsonProperty(required = true)
    public long buildDurationMillis;

    @JsonProperty(required = true)
    public String branchName;

    @JsonProperty(required = true)
    public String projectName;

    public String hostName;

    @JsonProperty(required = true)
    public String hostIP;

    @JsonProperty(required = true)
    public int maxConcurrency;

    @JsonProperty(required = true)
    public String jvmVersion;

    @JsonProperty(required = true)
    public int availableProcessors;

    @JsonProperty(required = true)
    public String gitHash;

    @JsonProperty(required = true)
    public Map<String,String> systemProperties;

    @JsonProperty(required = true)
    public Map<String,String> environment;

    @JsonProperty(required = true)
    public List<ArtifactCoords> coords;

    @JsonProperty(required = true)
    public List<Record> records;

    public static class Record {
        public int artifactIdx;
        public int pluginIdx;
        // --
        public String phase;
        public long startMillis;
        public long endMillis;

        public ArtifactCoords artifact(BuildResult r) {
            return r.coords.get(artifactIdx);
        }

        public ArtifactCoords plugin(BuildResult r) {
            return r.coords.get(pluginIdx);
        }
    }
}
