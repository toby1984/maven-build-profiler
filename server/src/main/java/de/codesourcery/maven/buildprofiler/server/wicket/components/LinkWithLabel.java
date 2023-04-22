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
package de.codesourcery.maven.buildprofiler.server.wicket.components;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public abstract class LinkWithLabel<T> extends Panel
{
    private final IModel<T> dataModel;
    private final IModel<?> displayModel;
    private Label label;

    public LinkWithLabel(String id, IModel<?> displayModel, IModel<T> dataModel)
    {
        super( id );
        Validate.notNull( displayModel, "displayModel must not be null" );
        Validate.notNull( dataModel, "model must not be null" );
        this.dataModel = dataModel;
        this.displayModel = displayModel;
        final AjaxLink<T> link = new AjaxLink<>( "link", dataModel )
        {

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                LinkWithLabel.this.onClick( target, getModelObject() );
            }
        };
        add( link );
        label = new Label( "label", displayModel );
        link.add( label );
    }

    public Label getLabel()
    {
        return label;
    }

    protected abstract void onClick(AjaxRequestTarget target, T modelObject);
}
