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
package de.codesourcery.maven.buildprofiler.server.wicket.components.charts;

import java.time.ZonedDateTime;
import java.util.function.Function;

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

    /**
     * Returns a mapping function that creates new items by dividing the Y axis value using a given factor.
     * @param factor factor to divide Y axis values by
     * @return
     */
    public static Function<DateXYDataItem,DateXYDataItem> divideYAxisValueMapping(double factor) {
        return a -> new DateXYDataItem( a.x, a.y / factor );
    }
}
