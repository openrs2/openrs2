-- @formatter:off

ALTER TABLE artifacts
    ADD COLUMN resolved_build_major INTEGER NULL,
    ADD COLUMN resolved_build_minor INTEGER NULL;

UPDATE artifacts
SET
    resolved_build_major = build_major,
    resolved_build_minor = build_minor;
