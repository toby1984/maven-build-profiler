--
-- Copyright © 2023 Tobias Gierke (tobias.gierke@code-sourcery.de)
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

BEGIN;

DROP SCHEMA IF EXISTS profiler CASCADE;
CREATE SCHEMA profiler;

-- profiler.hosts
DROP SEQUENCE IF EXISTS profiler.hosts_seq;
CREATE SEQUENCE profiler.hosts_seq;

DROP TABLE IF EXISTS profiler.hosts CASCADE;

CREATE TABLE profiler.hosts (
  host_id bigint PRIMARY KEY DEFAULT nextval('profiler.hosts_seq'),
  host_name text,
  host_ip inet UNIQUE NOT NULL 
);

-- profiler.builds
DROP SEQUENCE IF EXISTS profiler.builds_seq CASCADE;

CREATE SEQUENCE profiler.builds_seq;

DROP TABLE IF EXISTS build CASCADE;

CREATE TABLE builds (
  build_id bigint PRIMARY KEY DEFAULT nextval('profiler.builds_seq'),
  build_start_time timestamptz NOT NULL,
  build_duration_millis integer NOT NULL CHECK(build_duration_millis>0),
  host_id bigint NOT NULL REFERENCES profiler.hosts(host_id),
  project_name text NOT NULL,
  branch_name text NOT NULL,
  max_concurrency integer NOT NULL,
  jvm_version TEXT,
  available_processors integer NOT NULL,
  git_hash text,
  system_properties text,
  env_properties text
);

-- profiler.artifacts
DROP SEQUENCE IF EXISTS profiler.artifacts_seq CASCADE;
CREATE SEQUENCE profiler.artifacts_seq;

DROP TABLE IF EXISTS profiler.artifacts CASCADE;
CREATE TABLE profiler.artifacts (
  artifact_id bigint PRIMARY KEY DEFAULT nextval('profiler.artifacts_seq'),
  group_id_txt text NOT NULL,
  artifact_id_txt text NOT NULL
);

-- profiler.phases
DROP SEQUENCE IF EXISTS profiler.phases_seq CASCADE;
CREATE SEQUENCE profiler.phases_seq;

DROP TABLE IF EXISTS profiler.phases CASCADE;
CREATE TABLE profiler.phases (
  phase_id bigint PRIMARY KEY DEFAULT nextval('profiler.phases_seq'),
  phase_name text UNIQUE NOT NULL
);

-- profiler.records
DROP SEQUENCE IF EXISTS profiler.records_seq;
CREATE SEQUENCE profiler.records_seq;

DROP TABLE IF EXISTS profiler.records CASCADE;

CREATE TABLE profiler.records (
  record_id bigint PRIMARY KEY DEFAULT nextval('profiler.records_seq'),
  build_id bigint NOT NULL REFERENCES profiler.builds(build_id) ON DELETE CASCADE,
  phase_id bigint NOT NULL REFERENCES profiler.phases(phase_id) ON DELETE CASCADE, 
  plugin_artifact_id bigint NOT NULL REFERENCES profiler.artifacts(artifact_id) ON DELETE CASCADE,
  plugin_version text NOT NULL,
  artifact_id bigint NOT NULL REFERENCES profiler.artifacts(artifact_id) ON DELETE CASCADE,
  artifact_version text NOT NULL,
  start_time timestamptz NOT NULL,
  end_time timestamptz NOT NULL,
  CHECK( start_time <= end_time )
);

CREATE TABLE profiler.db_schema_version (
	row_id bigint PRIMARY KEY CHECK(row_id = 1),
        version text NOT NULL);

INSERT INTO profiler.db_schema_version VALUES(1,'1.0');

CREATE OR REPLACE FUNCTION profiler.assertdbschemaversion(expectedversion character varying)
 RETURNS void
 LANGUAGE plpgsql
AS $function$
DECLARE
    currentVersion text;
BEGIN
    SELECT version FROM profiler.db_schema_version WHERE row_id=1 INTO currentVersion;
    IF currentVersion <> expectedVersion THEN
      RAISE EXCEPTION 'DB schema version mismatch, expected % but got %',expectedVersion,currentVersion;
    END IF;
END;
$function$;

COMMIT;
