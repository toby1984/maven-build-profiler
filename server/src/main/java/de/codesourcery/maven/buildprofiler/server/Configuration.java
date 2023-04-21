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

import de.codesourcery.maven.buildprofiler.server.wicket.WicketApplication;
import org.apache.wicket.protocol.http.WicketFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import javax.servlet.DispatcherType;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import java.util.EnumSet;

@org.springframework.context.annotation.Configuration
@ComponentScan(basePackages = { "de.codesourcery.maven.buildprofiler.server" , "de.codesourcery.maven.buildprofiler.server.wicket" })
@ServletComponentScan(basePackages = "de.codesourcery.maven.buildprofiler.wicket")
public class Configuration
{

    @Value("${server.servlet.context-path}")
    public String ctxPath;

    @Bean
    public FilterRegistrationBean<WicketFilter> wicketFilter()
    {
        final WicketFilter wicketFilter = new WicketFilter() {
            @Override
            public void init(boolean isServlet, FilterConfig filterConfig) throws ServletException
            {
                setFilterPath( "/wicket" );
                super.init( isServlet, filterConfig );
            }
        };

        final FilterRegistrationBean<WicketFilter> filter = new FilterRegistrationBean<>( wicketFilter );
        filter.setName("wicket-filter");
        filter.setEnabled( true );
        filter.setOrder( 1 );
        filter.addUrlPatterns("/wicket/*");

        filter.setDispatcherTypes( EnumSet.allOf( DispatcherType.class ) );
        filter.setMatchAfter(true);
        filter.addInitParameter( "applicationClassName", WicketApplication.class.getName() );

        return filter;
    }
}
