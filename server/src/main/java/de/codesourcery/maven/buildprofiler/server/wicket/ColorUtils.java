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

import org.apache.commons.lang3.Validate;

import java.awt.Color;
import java.awt.Paint;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Static helper class to calculate the perceived difference ("delta E")
 * between two RGB values according to CIE76.
 *
 * See https://en.wikipedia.org/wiki/Color_difference
 *
 * Code taken from https://stackoverflow.com/questions/9018016/how-to-compare-two-colors-for-similarity-difference
 * @author Daniel Strebel,University of Zurich
 */
public final class ColorUtils
{
    // default chart colors
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_01 = new Color( 0x00, 0x00, 0x33);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_02 = new Color( 0x00, 0x00, 0xff);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_03 = new Color( 0xff, 0x00, 0x00);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_04 = new Color( 0x66, 0xff, 0xff);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_05 = new Color( 0x33, 0x00, 0x00);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_06 = new Color( 0x00, 0xcc, 0xcc);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_07 = new Color( 0x99, 0xcc, 0x00);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_08 = new Color( 0x66, 0x33, 0x66);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_09 = new Color( 0xcc, 0x33, 0x99);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_10 = new Color( 0x00, 0x66, 0x66);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_11 = new Color( 0xff, 0x99, 0x66);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_12 = new Color( 0x33, 0x66, 0xcc);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_13 = new Color( 0x66, 0x66, 0x00);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_14 = new Color( 0x66, 0x66, 0xff);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_15 = new Color( 0xcc, 0x66, 0x00);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_16 = new Color( 0xff, 0x66, 0xff);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_17 = new Color( 0x00, 0x99, 0xcc);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_18 = new Color( 0x66, 0x99, 0x00);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_19 = new Color( 0x66, 0x33, 0x33);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_20 = new Color( 0x00, 0x66, 0x99);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_21 = new Color( 0x00, 0x33, 0x00);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_22 = new Color( 0x99, 0x00, 0x00);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_23 = new Color( 0xff, 0xcc, 0x00);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_24 = new Color( 0x66, 0xff, 0x99);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_25 = new Color( 0x99, 0xcc, 0xcc);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_26 = new Color( 0x99, 0xff, 0x00);
    public static final Color CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_27 = new Color( 0xff, 0xff, 0x00);

    private static final List<Color> DEFAULT_CHART_COLORS = List.of(
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_01,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_02,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_03,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_04,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_05,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_06,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_07,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_08,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_09,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_10,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_11,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_12,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_13,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_14,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_15,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_16,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_17,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_18,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_19,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_20,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_21,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_22,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_23,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_24,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_25,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_26,
        CHART_INDICATOR_ANALYSIS_COLOR_INDICATOR_27
    );

    private ColorUtils() {
    }

    /**
     * Computes the difference between two RGB colors by converting them to the L*a*b scale and
     * comparing them using the CIE76 algorithm { http://en.wikipedia.org/wiki/Color_difference#CIE76}
     */
    public static double calcColorDifference(Color a, Color b)
    {
        final int[] lab1 = rgb2lab(a);
        final int[] lab2 = rgb2lab(b);
        return Math.sqrt(Math.pow(lab2[0] - lab1[0], 2) + Math.pow(lab2[1] - lab1[1], 2) + Math.pow(lab2[2] - lab1[2], 2));
    }

    private static int[] rgb2lab(Color color)
    {
        final int R = color.getRed();
        final int G = color.getGreen();
        final int B = color.getBlue();

        // http://www.brucelindbloom.com

        float r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
        float Ls, as, bs;
        float eps = 216.f / 24389.f;
        float k = 24389.f / 27.f;

        float Xr = 0.964221f;  // reference white D50
        float Yr = 1.0f;
        float Zr = 0.825211f;

        // RGB to XYZ
        r = R / 255.f; //R 0..1
        g = G / 255.f; //G 0..1
        b = B / 255.f; //B 0..1

        // assuming sRGB (D65)
        if (r <= 0.04045)
            r = r / 12;
        else
            r = (float) Math.pow((r + 0.055) / 1.055, 2.4);

        if (g <= 0.04045)
            g = g / 12;
        else
            g = (float) Math.pow((g + 0.055) / 1.055, 2.4);

        if (b <= 0.04045)
            b = b / 12;
        else
            b = (float) Math.pow((b + 0.055) / 1.055, 2.4);


        X = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b;
        Y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b;
        Z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b;

        // XYZ to Lab
        xr = X / Xr;
        yr = Y / Yr;
        zr = Z / Zr;

        if (xr > eps)
            fx = (float) Math.pow(xr, 1 / 3.);
        else
            fx = (float) ((k * xr + 16.) / 116.);

        if (yr > eps)
            fy = (float) Math.pow(yr, 1 / 3.);
        else
            fy = (float) ((k * yr + 16.) / 116.);

        if (zr > eps)
            fz = (float) Math.pow(zr, 1 / 3.);
        else
            fz = (float) ((k * zr + 16.) / 116);

        Ls = (116 * fy) - 16;
        as = 500 * (fx - fy);
        bs = 200 * (fy - fz);

        int[] lab = new int[3];
        lab[0] = (int) (2.55 * Ls + .5);
        lab[1] = (int) (as + .5);
        lab[2] = (int) (bs + .5);
        return lab;
    }

    /**
     * Returns an iterator that returns an (at least theoretically) unlimited
     * amount of colors to be used as series paint (dataset color) when rendering charts.
     *
     * The returned iterator attempts (within reason) to generate colors that are visually distinct
     * as much as possible from any colors that were already returned by a previous invocation
     * of {@link Iterator#next()}.
     *
     * @return iterator that returns random colors suitable for rendering charts with.
     */
    public static Iterator<Color> getChartColorSupplier() {
        return getChartColorSupplier( DEFAULT_CHART_COLORS );
    }

    /**
     * Returns an iterator that returns an (at least theoretically) unlimited
     * amount of colors to be used as series paint (dataset color) when rendering charts.
     *
     * The returned iterator attempts (within reason) to generate colors that are visually distinct
     * as much as possible from any colors that were already returned by a previous invocation
     * of {@link Iterator#next()}.
     *
     * @param defaultColors colors to return first, random ones will be returned after running out of detals.
     * @return iterator that returns random colors suitable for rendering charts with.
     */
    public static Iterator<Color> getChartColorSupplier(List<Color> defaultColors)
    {
        Validate.notEmpty( defaultColors, "Default colors must not be empty" );
        return new Iterator<>() {

            private final Random rnd = new Random( 0xdeadbeef );
            private final Iterator<Color> it = DEFAULT_CHART_COLORS.iterator();
            private final Set<Color> existing = new HashSet<>();

            @Override
            public boolean hasNext()
            {
                return true;
            }

            @Override
            public Color next()
            {
                Color result = null;
                if ( it.hasNext() ) { // do we still have default colors to choose from?
                    result = it.next();
                } else
                {
                    // ran out of default colors, generate 20 random colors
                    // and return the one that is farthest away (according to "delta E" algorithm, CIE76)
                    // from all colors we've already returned.
                    final List<Color> colors = IntStream.range( 0, 20 )
                        .mapToObj( idx -> rndUnusedColor() ).collect( Collectors.toList() );

                    double bestDistance = 0;
                    for ( final Color c : colors )
                    {
                        final double d = existing.stream().mapToDouble( x -> ColorUtils.calcColorDifference( x, c )
                        ).min().orElse( 0 );
                        if ( result == null || d > bestDistance )
                        {
                            result = c;
                            bestDistance = d;
                        }
                    }
                }
                existing.add( result );
                return result;
            }

            private Color rndUnusedColor()
            {
                Color result;
                do
                {
                    result = new Color(
                        rnd.nextInt( 256 ),
                        rnd.nextInt( 256 ),
                        rnd.nextInt( 256 ) );
                } while (existing.contains( result ));
                return result;
            }
        };
    }
}