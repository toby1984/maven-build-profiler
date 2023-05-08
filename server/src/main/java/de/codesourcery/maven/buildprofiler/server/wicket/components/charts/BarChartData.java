package de.codesourcery.maven.buildprofiler.server.wicket.components.charts;

import org.apache.commons.lang3.Validate;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BarChartData
{
    public final List<String> labels = new ArrayList<>();
    public final List<DataSet> datasets = new ArrayList<>();

    public record DataSet(String name, Color color, List<Double> data) {

        public DataSet
        {
            Validate.notBlank( name, "name must not be null or blank");
            Validate.notNull( color, "color must not be null" );
            Validate.notNull( data, "data must not be null" );
        }
    }

    public void addLabel(String label) {
        Validate.notBlank( label, "label must not be null or blank");
        Validate.isTrue( !labels.contains( label ), "Duplicate label '" + label + "'" );
        this.labels.add( label );
    }

    public void addLabels(String label1,String... additional) {
        Validate.notNull( additional, "additional must not be null" );
        addLabel( label1 );
        Arrays.stream( additional ).forEach( this::addLabel );
    }

    public void add(DataSet ds)
    {
        Validate.notNull( ds, "ds must not be null" );
        Validate.isTrue( this.datasets.stream().noneMatch( x -> x.name.equals( ds.name ) ) , "Duplicate dataset name '"+ds.name+"'" );
        Validate.isTrue( this.datasets.stream().noneMatch( x -> x.color.equals( ds.color ) ) , "Duplicate dataset color '"+ds.color+"'" );
        this.datasets.add( ds );
    }
}