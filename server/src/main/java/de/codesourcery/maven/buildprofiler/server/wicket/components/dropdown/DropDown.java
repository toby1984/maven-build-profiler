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