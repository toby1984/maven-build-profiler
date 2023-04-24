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

import de.codesourcery.maven.buildprofiler.shared.ArtifactCoords;
import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.util.Objects;

/**
 * An artifacts group ID and artifact ID.
 *
 * @param groupIdText
 * @param artifactIdText
 *
 * @author tobias.gierke@code-sourcery.de
 */
public record ArtifactId(String groupIdText, String artifactIdText) implements Serializable
{
    public ArtifactId
    {
        Validate.notBlank( groupIdText, "groupIdText must not be null or blank");
        Validate.notBlank( artifactIdText, "artifactIdText must not be null or blank");
    }

    public static ArtifactId of(ArtifactCoords coords) {
        return new ArtifactId(coords.groupId(), coords.artifactId());
    }

    public boolean matches(Artifact art) {
        return Objects.equals( groupIdText, art.groupId ) &&
            Objects.equals( artifactIdText, art.artifactId );
    }

    @Override
    public String toString()
    {
        return getUIString();
    }

    public String getUIString()
    {
        return groupIdText+":"+artifactIdText;
    }
}
