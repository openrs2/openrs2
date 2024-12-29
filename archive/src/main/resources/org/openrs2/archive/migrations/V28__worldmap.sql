-- @formatter:off

ALTER TYPE artifact_type ADD VALUE 'worldmap';
ALTER TYPE artifact_format ADD VALUE 'jag';

ALTER TABLE artifacts ADD COLUMN resolved_timestamp TIMESTAMPTZ NULL;

UPDATE artifacts SET resolved_timestamp = timestamp;
