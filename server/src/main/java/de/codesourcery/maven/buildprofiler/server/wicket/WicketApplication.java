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
package de.codesourcery.maven.buildprofiler.server.wicket;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class WicketApplication extends WebApplication
{
    @Override
    protected void init()
    {
        super.init();
        getCspSettings().blocking().disabled();
        final WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext( getServletContext() );
        Validate.notNull( ctx, "ctx must not be null" );
        getComponentInstantiationListeners().add( new SpringComponentInjector( this, ctx ) );
    }

    @Override
    public Class<? extends Page> getHomePage()
    {
        return HomePage.class;
    }
}
