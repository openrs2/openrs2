-- @formatter:off

CREATE TYPE crc_table_format AS ENUM (
    'original',
    'checksum'
);

ALTER TABLE crc_tables DROP CONSTRAINT crc_tables_blob_id_key;

ALTER TABLE crc_tables ADD COLUMN format crc_table_format NOT NULL DEFAULT 'checksum';
ALTER TABLE crc_tables ALTER COLUMN format DROP DEFAULT;

CREATE UNIQUE INDEX crc_tables_blob_id_format_key ON crc_tables (blob_id, format);
