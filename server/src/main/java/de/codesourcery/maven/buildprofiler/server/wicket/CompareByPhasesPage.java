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

import de.codesourcery.maven.buildprofiler.server.db.DAO;
import de.codesourcery.maven.buildprofiler.server.model.Build;
import de.codesourcery.maven.buildprofiler.server.wicket.components.CompareByPhasesPanel;
import org.apache.commons.lang3.Validate;

import java.util.List;

public class CompareByPhasesPage extends AbstractBasePage
{
    private final List<Build> toCompare;
    private final DAO.SearchCriteria criteria;

    public CompareByPhasesPage(List<Build> toCompare, DAO.SearchCriteria criteria)
    {
        Validate.isTrue( !toCompare.isEmpty(),"need builds to compare" );
        Validate.notNull( criteria, "criteria must not be null" );
        this.criteria = criteria;
        this.toCompare = toCompare;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        add( new CompareByPhasesPanel( "comparePanel", () -> toCompare , criteria) );
    }
}
