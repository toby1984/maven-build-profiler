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
package de.codesourcery.maven.buildprofiler.server.wicket.components.charts;

import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

public class DataSet<X extends XYDataItem>
{
    private List<X> items = new ArrayList<>();

    public DataSet() {
    }

    public DataSet(List<X> items)
    {
        Validate.notNull( items, "items must not be null" );
        this.items = items;
    }

    public void add(X item) {
        Validate.notNull( item, "item must not be null" );
        this.items.add( item );
    }

    public List<X> getItems()
    {
        return items;
    }
}
