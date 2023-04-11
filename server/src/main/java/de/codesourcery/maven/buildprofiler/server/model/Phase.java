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

import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Phase
{
    /*
     * Note that the order of these enum constants
     * is used by the UI to also enforce a total ordering
     * on the phases ; so don't mess with it !
     */

    // clean lifecycle
    PRE_CLEAN( "pre-clean" ),
    CLEAN( "clean" ),
    POST_CLEAN( "post-clean" ),
    // regular lifecycle
    VALIDATE( "validate" ),
    INITIALIZE( "initialize" ),
    GENERATE_SOURCES( "generate-sources" ),
    PROCESS_SOURCES( "process-sources" ),
    GENERATE_RESOURCES( "generate-resources" ),
    PROCESS_RESOURCES( "process-resources" ),
    COMPILE( "compile" ),
    PROCESS_CLASSES( "process-classes" ),
    GENERATE_TEST_SOURCES( "generate-test-sources" ),
    PROCESS_TEST_SOURCES( "process-test-sources" ),
    GENERATE_TEST_RESOURCES( "generate-test-resources" ),
    PROCESS_TEST_RESOURCES( "process-test-resources" ),
    TEST_COMPILE( "test-compile" ),
    PROCESS_TEST_CLASSES( "process-test-classes" ),
    TEST( "test" ),
    PREPARE_PACKAGE( "prepare-package" ),
    PACKAGE( "package" ),
    PRE_INTEGRATION_TEST( "pre-integration-test" ),
    INTEGRATION_TEST( "integration-test" ),
    POST_INTEGRATION_TEST( "post-integration-test" ),
    VERIFY( "verify" ),
    INSTALL( "install" ),
    DEPLOY( "deploy" ),
    // site plugin
    PRE_SITE( "pre-site" ),
    SITE( "site" ),
    POST_SITE( "post-site" ),
    SITE_DEPLOY( "site-deploy" ),
        ;

    public final String name;
    public final String dbId;

    private static final class Helper
    {
        public static final Map<String, Phase> map = new HashMap<>();
    }

    Phase(String name)
    {
        this.name = name;
        this.dbId = name;
        Helper.map.put( name, this );
    }

    public String getDbId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public static Phase fromString(String name)
    {
        Validate.notBlank( name, "name must not be null or blank");
        final Phase result = Helper.map.get( name );
        if ( result == null ) {
            throw new IllegalArgumentException( "Unknown phase '" + name + "'" );
        }
        return result;
    }

    public static List<LifecyclePhase> sort(List<LifecyclePhase> toSort)
    {
        final List<LifecyclePhase> result = new ArrayList<>(toSort);
        result.sort( (a,b) -> {
            int o1 = Phase.fromString( a.name ).ordinal();
            int o2 = Phase.fromString( b.name ).ordinal();
            return Integer.compare( o1, o2 );
        });
        return result;
    }
}