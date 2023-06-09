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
package de.codesourcery.maven.buildprofiler.server.wicket.components.datatable;

import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.resource.CssResourceReference;

import java.util.List;

public class MyDataTable<A,B> extends DefaultDataTable<A,B>
{
    private static final CssResourceReference CSS = new CssResourceReference( MyDataTable.class, "datatable.css" );

    /**
     * Constructor
     *
     * @param id           component id
     * @param iColumns     list of columns
     * @param dataProvider data provider
     * @param rowsPerPage  number of rows per page
     */
    public MyDataTable(String id, List<? extends IColumn<A, B>> iColumns, ISortableDataProvider<A, B> dataProvider, int rowsPerPage)
    {
        super( id, iColumns, dataProvider, rowsPerPage );
    }

    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead( response );
        response.render( CssReferenceHeaderItem.forReference( CSS ) );
    }
}