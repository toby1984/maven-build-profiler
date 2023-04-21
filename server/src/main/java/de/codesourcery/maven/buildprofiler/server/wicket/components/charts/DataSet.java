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
