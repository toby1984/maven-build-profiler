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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;

/**
 * NOTE: This class gets instantiated by Spring because its listed in META-INF/spring.factories.
 */
public class ConfigPropertiesFileResolver implements ApplicationListener<ApplicationEnvironmentPreparedEvent>
{
    private static final Logger LOG = LogManager.getLogger( ConfigPropertiesFileResolver.class );

    public static final String SYSTEM_PROPERTY = "mavenBuildProfiler.config";
    public static final String DEFAULT_VALUE = "classpath:/mavenBuildProfiler.properties";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event)
    {
        final String name = System.getProperty( SYSTEM_PROPERTY, DEFAULT_VALUE );
        final String path;
        if ( name.startsWith("/") ) {
            path = "file://" + name;
        } else {
            path = name;
        }
        LOG.info( "Trying to use Spring Boot application properties from '" + path+"'. Set -D"+SYSTEM_PROPERTY+" to override that location." );
        final ConfigurableEnvironment environment = event.getEnvironment();
        try
        {
            environment.getPropertySources().addFirst( new ResourcePropertySource( path ) );
        }
        catch( IOException e )
        {
            throw new RuntimeException( "Failed to load application properties from '" + path + "'", e );
        }
    }
}