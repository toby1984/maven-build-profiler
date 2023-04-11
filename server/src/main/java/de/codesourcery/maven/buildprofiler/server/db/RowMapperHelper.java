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
package de.codesourcery.maven.buildprofiler.server.db;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;

public interface RowMapperHelper<T> extends RowMapper<T>
{
    default ZonedDateTime dateTime(String columnName, ResultSet rs) throws SQLException
    {
        final Timestamp value = rs.getTimestamp( columnName );
        return value == null ? null : ZonedDateTime.ofInstant( value.toInstant(), ZoneId.systemDefault() );
    }

    default Duration duration(String columnName, TemporalUnit unit, ResultSet rs) throws SQLException
    {
        final int value = rs.getInt( columnName );
        return rs.wasNull() ? null : Duration.of( value, unit );
    }
}
