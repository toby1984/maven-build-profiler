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
package de.codesourcery.maven.buildprofiler.server.wicket.components.tooltip;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.codesourcery.maven.buildprofiler.server.wicket.JsonResponseHandler;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.string.Strings;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TooltipBehaviour extends AbstractDefaultAjaxBehavior
{
    private static final JavaScriptResourceReference JS =
        new JavaScriptResourceReference( TooltipBehaviour.class, "tooltip.js" );

    private static final CssResourceReference CSS =
        new CssResourceReference( TooltipBehaviour.class, "tooltip.css" );

    private boolean escapeModelStrings = true;
    private final IModel<String> tooltipModel;
    private final int widthInPixels;

    public TooltipBehaviour(IModel<String> tooltipModel, int widthInPixels)
    {
        Validate.notNull( tooltipModel, "tooltipModel must not be null" );
        Validate.isTrue( widthInPixels > 0, "width must be > 0 but was "+widthInPixels );
        this.widthInPixels = widthInPixels;
        this.tooltipModel = tooltipModel;
    }

    public static TooltipBehaviour of(IModel<String> tooltipModel, int widthInPixels) {
        return new TooltipBehaviour( tooltipModel, widthInPixels );
    }

    public TooltipBehaviour setEscapeModelStrings(boolean yesNo) {
        this.escapeModelStrings = yesNo;
        return this;
    }

    @Override
    protected void onComponentTag(ComponentTag tag)
    {
        tag.getAttributes().put( "callbackUrl", getCallbackUrl() );
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response)
    {
        super.renderHead( component, response );
        response.render( JavaScriptHeaderItem.forReference( JS ) );
        response.render( CssHeaderItem.forReference( CSS ) );
        final String componentId = getComponent().getMarkupId( true );
        final String initializer = "tooltip.init('" + componentId+"')";
        response.render( OnDomReadyHeaderItem.forScript( initializer) );
    }

    @Override
    protected void respond(AjaxRequestTarget target)
    {
        final String json;
        try
        {
            final Map<String,String> response = new HashMap<>();
            final String text = tooltipModel.getObject();
            final String escaped = text == null ? null : escapeModelStrings ?
                Strings.escapeMarkup(text, false, false).toString() : text;
            response.put( "tooltipText", escaped );
            response.put( "width", Integer.toString( widthInPixels ) );
            json = new ObjectMapper().writeValueAsString( response );
        }
        catch( IOException e )
        {
            throw new RuntimeException( e );
        }
        JsonResponseHandler.respond( json );
    }
}