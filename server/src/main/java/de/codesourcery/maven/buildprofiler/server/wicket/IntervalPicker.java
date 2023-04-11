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

import de.codesourcery.maven.buildprofiler.common.Interval;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.wicketstuff.wiquery.ui.datepicker.DatePicker;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class IntervalPicker extends FormComponentPanel<Interval>
{
    private MyDatePicker startDatePicker;
    private MyDatePicker endDatePicker;

    private final class MyDatePicker extends DatePicker<LocalDate>
    {
        public MyDatePicker(String id, IModel<LocalDate> model)
        {
            super( id, model, LocalDate.class );
            setDateFormat( "yy-mm-dd" );
            setFirstDay((short) 1);
            setShowOn(ShowOnEnum.BOTH);
            setDayNamesMin(getDayNamesShort());
            setRequired(true);
        }

        @Override
        public <C> IConverter<C> getConverter(Class<C> type)
        {
            return (IConverter<C>) new IConverter<LocalDate>() {

                private DateTimeFormatter getFormatter()
                {
                    return DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        .withZone( getTimeZone() );
                }

                @Override
                public LocalDate convertToObject(String value, Locale locale) throws ConversionException
                {
                    if ( StringUtils.isBlank( value ) )
                    {
                        return null;
                    }
                    return LocalDate.parse( value, getFormatter() );
                }

                @Override
                public String convertToString(LocalDate value, Locale locale)
                {
                    return value == null ? null : getFormatter().format( value );
                }
            };
        }
    }

    public IntervalPicker(String id, IModel<Interval> model)
    {
        super( id, model );
        Validate.notNull( model, "model must not be null" );
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        // start date picker
        startDatePicker = new MyDatePicker("startDatePicker", new Model<>() );
        add( startDatePicker );

        // end date picker
        endDatePicker = new MyDatePicker("endDatePicker",new Model<>());
        add( endDatePicker );
        onModelChanged();
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        startDatePicker.setModelObject( getModelObject().start.toLocalDate() );
        endDatePicker.setModelObject( getModelObject().end.toLocalDate() );
    }

    private ZoneId getTimeZone() {
        return ZoneId.systemDefault();
    }

    @Override
    public void convertInput()
    {
        final ZoneId timezone = getTimeZone();
        final ZonedDateTime start = startDatePicker.getConvertedInput().atStartOfDay( timezone );
        final ZonedDateTime end = endDatePicker.getConvertedInput().plusDays( 1 ).atStartOfDay( timezone );
        setConvertedInput( new Interval( start, end ) );
    }
}
