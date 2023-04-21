package de.codesourcery.maven.buildprofiler.server.wicket.components.charts;

import java.time.ZonedDateTime;

public record DateXYDataItem(ZonedDateTime x, double y) implements XYDataItem
{
    @Override
    public double getX()
    {
        return x.toInstant().toEpochMilli();
    }

    @Override
    public double getY()
    {
        return  y;
    }
}
