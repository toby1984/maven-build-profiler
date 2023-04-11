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
package de.codesourcery.maven.buildprofiler.server.wicket;

import org.knowm.xchart.VectorGraphicsEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.colors.XChartSeriesColors;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Component
public class ChartHelper
{
    public record ChartData<A,B>(A x, B y) {}

    public String toSVG(XYChart chart)
    {
        try
        {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            VectorGraphicsEncoder.saveVectorGraphic(chart, bos, VectorGraphicsEncoder.VectorGraphicsFormat.SVG);
            return bos.toString( StandardCharsets.UTF_8 );
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public XYChart createTemporalLineChart(List<ChartData<ZonedDateTime,? extends Number>> data, String datePattern)
    {
        // Create Chart
        XYChart chart = new XYChartBuilder().width(800).height(600).title("Build Time").xAxisTitle("Time").yAxisTitle("Duration (seconds)")
            .build();

        // Customize Chart
        chart.getStyler().setPlotBackgroundColor( Color.GRAY );
        chart.getStyler().setPlotGridLinesColor(new Color(255, 255, 255));
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setLegendBackgroundColor(Color.PINK);
        chart.getStyler().setChartFontColor(Color.MAGENTA);
        chart.getStyler().setChartTitleBoxBackgroundColor(new Color(0, 222, 0));
        chart.getStyler().setChartTitleBoxVisible(true);
        chart.getStyler().setChartTitleBoxBorderColor(Color.BLACK);
        chart.getStyler().setPlotGridLinesVisible(false);

        chart.getStyler().setAxisTickPadding(20);

        chart.getStyler().setAxisTickMarkLength(15);

        chart.getStyler().setPlotMargin(20);

        chart.getStyler().setChartTitleFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        chart.getStyler().setLegendFont(new Font(Font.SERIF, Font.PLAIN, 18));
        chart.getStyler().setLegendPosition( Styler.LegendPosition.InsideSE);
        chart.getStyler().setLegendSeriesLineLength(12);
        chart.getStyler().setAxisTitleFont(new Font(Font.SANS_SERIF, Font.ITALIC, 18));
        chart.getStyler().setAxisTickLabelsFont(new Font(Font.SERIF, Font.PLAIN, 11));
        chart.getStyler().setDatePattern(datePattern);
        chart.getStyler().setDecimalPattern("#0.000");
        chart.getStyler().setLocale(Locale.US);

        // Series
        if ( ! data.isEmpty() )
        {
            final List<Date> xData = new ArrayList<>(data.size());
            final List<Number> yData = new ArrayList<>(data.size());

            for ( ChartData<ZonedDateTime, ? extends Number> item : data )
            {
                xData.add( new Date( item.x.toInstant().toEpochMilli() ));
                yData.add( item.y );
            }

            final XYSeries series = chart.addSeries("Build time", xData, yData);
            series.setLineColor(XChartSeriesColors.BLUE);
            series.setMarkerColor(Color.ORANGE);
            series.setMarker(SeriesMarkers.CIRCLE);
            series.setLineStyle(SeriesLines.SOLID);
        }

        return chart;
    }
}
