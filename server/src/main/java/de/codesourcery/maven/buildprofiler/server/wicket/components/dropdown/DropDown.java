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
package de.codesourcery.maven.buildprofiler.server.wicket.components.dropdown;

import java.util.List;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

public class DropDown<T> extends FormComponentPanel<T>
{
    private static final ResourceReference JAVASCRIPT =
        new PackageResourceReference( DropDown.class, "dropdown.js" );

    private final IModel<List<T>> choicesModel;
    private final IModel<T> selectionModel;
    private DropDownChoice<T> choice;

    public DropDown(String id, IModel<T> selectionModel, IModel<List<T>> choicesModel)
    {
        super( id, selectionModel );
        this.selectionModel = selectionModel;
        this.choicesModel = choicesModel;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        this.choice = new DropDownChoice<>( "dropdown", selectionModel, choicesModel, createChoiceRenderer() );
        this.choice.setOutputMarkupPlaceholderTag( true );
        add( choice );
    }

    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead( response );
        response.render( JavaScriptReferenceHeaderItem.forReference( JAVASCRIPT ) );
        response.render( OnDomReadyHeaderItem.forScript( "dropdown.attach('" + this.choice.getMarkupId( true ) + "')" ) );
    }

    protected IChoiceRenderer<T> createChoiceRenderer() {
        return new ChoiceRenderer<>();
    }
}