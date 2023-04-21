package de.codesourcery.maven.buildprofiler.server.wicket.components.charts;

public record NumericXYDataItem(double x, double y) implements XYDataItem
{
    @Override
    public double getX()
    {
        return x;
    }

    @Override
    public double getY()
    {
        return y;
    }
}
