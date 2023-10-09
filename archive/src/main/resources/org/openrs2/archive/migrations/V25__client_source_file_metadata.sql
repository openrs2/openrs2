-- @formatter:off

ALTER TABLE artifact_sources
    ADD COLUMN file_name TEXT NULL,
    ADD COLUMN timestamp TIMESTAMPTZ NULL;
