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