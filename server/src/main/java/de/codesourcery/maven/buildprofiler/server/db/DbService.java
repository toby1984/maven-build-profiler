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

import com.fasterxml.jackson.core.JsonProcessingException;
import de.codesourcery.maven.buildprofiler.common.BuildResult;
import de.codesourcery.maven.buildprofiler.server.model.Artifact;
import de.codesourcery.maven.buildprofiler.server.model.ArtifactId;
import de.codesourcery.maven.buildprofiler.server.model.Build;
import de.codesourcery.maven.buildprofiler.server.model.Host;
import de.codesourcery.maven.buildprofiler.server.model.LifecyclePhase;
import de.codesourcery.maven.buildprofiler.server.model.Record;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface DbService
{
    // Record
    List<Record> getRecords(long buildId);
    Map<Long,List<Record>> getRecords(Set<Long> buildIds);
    void saveRecords(List<Record> toInsert);

    // Artifact
    List<Artifact> getArtifacts(Set<ArtifactId> ids);
    List<Artifact> getArtifactsByIDs(Set<Long> ids);
    void saveNewArtifacts(List<Artifact> toInsert);

    // LifecyclePhase
    Map<String, LifecyclePhase> getPhases(Set<String> names);
    List<LifecyclePhase> getPhasesByIDs(Set<Long> phaseIds);
    List<LifecyclePhase> getPhases();
    void saveNewPhases(List<LifecyclePhase> phases);

    // BuildResult
    void save(BuildResult result) throws JsonProcessingException, UnknownHostException;

    // Build
    int getBuildCount(DAO.SearchCriteria criteria);
    List<Build> getBuild(DAO.SearchCriteria criteria);
    List<Build> getBuilds(Set<Long> buildIds);
    void saveNew(List<Build> toInsert);

    // Hosts
    List<Host> getHosts();
    Optional<Host> getHostByIP(InetAddress hostIP);
    void saveOrUpdate(Host host);

    // misc
    List<String> getDistinctProjects();
    List<String> getBranchNames(String projectName,Host host);
}
