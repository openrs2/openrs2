-- @formatter:off
CREATE TABLE blobs (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    crc32 INTEGER NOT NULL,
    whirlpool BYTEA UNIQUE NOT NULL,
    data BYTEA NOT NULL
);

CREATE INDEX ON blobs USING HASH (crc32);

CREATE TABLE caches (
    id SERIAL PRIMARY KEY NOT NULL
);

INSERT INTO caches (id)
SELECT id FROM master_indexes;

SELECT setval('caches_id_seq', MAX(id)) FROM caches;

ALTER TABLE master_indexes
    ADD FOREIGN KEY (id) REFERENCES caches (id),
    ALTER COLUMN id DROP DEFAULT;

DROP SEQUENCE master_indexes_id_seq;

ALTER TABLE updates RENAME master_index_id TO cache_id;

ALTER TABLE updates
    DROP CONSTRAINT updates_master_index_id_fkey,
    ADD FOREIGN KEY (cache_id) REFERENCES caches (id);

ALTER TABLE sources RENAME master_index_id TO cache_id;

ALTER TABLE sources
    DROP CONSTRAINT sources_master_index_id_fkey,
    ADD FOREIGN KEY (cache_id) REFERENCES caches (id);

CREATE TABLE crc_tables (
    id INTEGER PRIMARY KEY NOT NULL REFERENCES caches (id),
    blob_id BIGINT UNIQUE NOT NULL REFERENCES blobs (id)
);

CREATE TABLE crc_table_archives (
    crc_table_id INTEGER NOT NULL REFERENCES crc_tables (id),
    archive_id uint1 NOT NULL,
    crc32 INTEGER NOT NULL,
    PRIMARY KEY (crc_table_id, archive_id)
);

CREATE TABLE archives (
    archive_id uint1 NOT NULL,
    blob_id BIGINT NOT NULL REFERENCES blobs (id),
    PRIMARY KEY (archive_id, blob_id)
);

CREATE TABLE version_lists (
    blob_id BIGINT PRIMARY KEY NOT NULL REFERENCES blobs (id)
);

CREATE TABLE version_list_files (
    blob_id BIGINT NOT NULL REFERENCES version_lists (blob_id),
    index_id uint1 NOT NULL,
    file_id uint2 NOT NULL,
    version uint2 NOT NULL,
    crc32 INTEGER NOT NULL,
    PRIMARY KEY (blob_id, index_id, file_id)
);

CREATE TABLE version_list_maps (
    blob_id BIGINT NOT NULL REFERENCES version_lists (blob_id),
    map_square uint2 NOT NULL,
    map_file_id uint2 NOT NULL,
    loc_file_id uint2 NOT NULL,
    free_to_play BOOLEAN NOT NULL,
    PRIMARY KEY (blob_id, map_square)
);

CREATE TABLE files (
    index_id uint1 NOT NULL,
    file_id uint2 NOT NULL,
    version uint2 NOT NULL,
    blob_id BIGINT NOT NULL REFERENCES blobs (id),
    PRIMARY KEY (index_id, file_id, version, blob_id)
);

CREATE TABLE source_archives (
    source_id INTEGER NOT NULL REFERENCES sources (id),
    archive_id uint1 NOT NULL,
    blob_id BIGINT NOT NULL REFERENCES blobs (id),
    PRIMARY KEY (source_id, archive_id, blob_id),
    FOREIGN KEY (archive_id, blob_id) REFERENCES archives (archive_id, blob_id)
);

CREATE INDEX ON source_archives (archive_id, blob_id);

CREATE TABLE source_files (
    source_id INTEGER NOT NULL REFERENCES sources (id),
    index_id uint1 NOT NULL,
    file_id uint2 NOT NULL,
    version uint2 NOT NULL,
    blob_id BIGINT NOT NULL REFERENCES blobs (id),
    PRIMARY KEY (source_id, index_id, file_id, version, blob_id),
    FOREIGN KEY (index_id, file_id, version, blob_id) REFERENCES files (index_id, file_id, version, blob_id)
);

CREATE INDEX ON source_files (index_id, file_id, version, blob_id);

CREATE FUNCTION resolve_archive(_archive_id uint1, _crc32 INTEGER) RETURNS SETOF blobs AS $$
    SELECT b.*
    FROM archives a
    JOIN blobs b ON b.id = a.blob_id
    WHERE a.archive_id = _archive_id AND b.crc32 = _crc32
    ORDER BY b.id ASC
    LIMIT 1;
$$ LANGUAGE SQL STABLE PARALLEL SAFE ROWS 1;

CREATE FUNCTION resolve_file(_index_id uint1, _file_id uint2, _version uint2, _crc32 INTEGER) RETURNS SETOF blobs AS $$
    SELECT b.*
    FROM files f
    JOIN blobs b on b.id = f.blob_id
    WHERE f.index_id = _index_id AND f.file_id = _file_id AND f.version = _version AND b.crc32 = _crc32
    ORDER BY b.id ASC
    LIMIT 1;
$$ LANGUAGE SQL STABLE PARALLEL SAFE ROWS 1;

CREATE VIEW resolved_archives AS
SELECT c.id AS crc_table_id, a.archive_id, b.data, b.id AS blob_id
FROM crc_tables c
JOIN crc_table_archives a ON a.crc_table_id = c.id
JOIN resolve_archive(a.archive_id, a.crc32) b ON TRUE;

CREATE VIEW resolved_files (crc_table_id, index_id, file_id, version, data) AS
WITH a AS NOT MATERIALIZED (
    SELECT crc_table_id, archive_id, data, blob_id
    FROM resolved_archives
)
SELECT a.crc_table_id, 0::uint1, a.archive_id, NULL, a.data
FROM a
UNION ALL
SELECT a.crc_table_id, vf.index_id, vf.file_id, vf.version, f.data
FROM a
JOIN version_lists v ON v.blob_id = a.blob_id
JOIN version_list_files vf ON vf.blob_id = v.blob_id
JOIN resolve_file(vf.index_id, vf.file_id, vf.version, vf.crc32) f ON TRUE
WHERE a.archive_id = 5;

ALTER VIEW collisions RENAME TO colliding_groups;

CREATE VIEW colliding_archives (archive_id, crc32, blobs) AS
SELECT
    a.archive_id,
    b.crc32,
    array_agg(DISTINCT b.id ORDER BY b.id ASC)
FROM archives a
JOIN blobs b ON b.id = a.blob_id
GROUP BY a.archive_id, b.crc32
HAVING COUNT(DISTINCT b.id) > 1;

CREATE VIEW colliding_files (index_id, file_id, version, crc32, blobs) AS
SELECT
    f.index_id,
    f.file_id,
    f.version,
    b.crc32,
    array_agg(DISTINCT b.id ORDER BY b.id ASC)
FROM files f
JOIN blobs b ON b.id = f.blob_id
GROUP BY f.index_id, f.file_id, f.version, b.crc32
HAVING COUNT(DISTINCT b.id) > 1;

CREATE MATERIALIZED VIEW version_list_stats AS
SELECT
    v.blob_id,
    COUNT(*) FILTER (WHERE b.id IS NOT NULL) AS valid_files,
    COUNT(*) AS files,
    SUM(length(b.data) + 2) FILTER (WHERE b.id IS NOT NULL) AS size,
    SUM(group_blocks(vf.file_id, length(b.data) + 2)) AS blocks
FROM version_lists v
JOIN version_list_files vf ON vf.blob_id = v.blob_id
LEFT JOIN resolve_file(vf.index_id, vf.file_id, vf.version, vf.crc32) b ON TRUE
GROUP BY v.blob_id
WITH NO DATA;

CREATE UNIQUE INDEX ON version_list_stats (blob_id);

CREATE MATERIALIZED VIEW crc_table_stats AS
SELECT
    c.id AS crc_table_id,
    COUNT(*) FILTER (WHERE b.id IS NOT NULL AND a.crc32 <> 0) AS valid_archives,
    COUNT(*) FILTER (WHERE a.crc32 <> 0) AS archives,
    SUM(COALESCE(s.valid_files, 0)) AS valid_files,
    SUM(COALESCE(s.files, 0)) AS files,
    SUM(COALESCE(s.size, 0)) + SUM(COALESCE(length(b.data), 0)) AS size,
    SUM(COALESCE(s.blocks, 0)) + SUM(COALESCE(group_blocks(a.archive_id, length(b.data)), 0)) AS blocks
FROM crc_tables c
LEFT JOIN crc_table_archives a ON a.crc_table_id = c.id
LEFT JOIN resolve_archive(a.archive_id, a.crc32) b ON TRUE
LEFT JOIN version_list_stats s ON s.blob_id = b.id
GROUP BY c.id
WITH NO DATA;

CREATE UNIQUE INDEX ON crc_table_stats (crc_table_id);

CREATE VIEW cache_stats AS
SELECT
    c.id AS cache_id,
    COALESCE(ms.valid_indexes, cs.valid_archives) AS valid_indexes,
    COALESCE(ms.indexes, cs.archives) AS indexes,
    COALESCE(ms.valid_groups, cs.valid_files) AS valid_groups,
    COALESCE(ms.groups, cs.files) AS groups,
    COALESCE(ms.valid_keys, 0) AS valid_keys,
    COALESCE(ms.keys, 0) AS keys,
    COALESCE(ms.size, cs.size) AS size,
    COALESCE(ms.blocks, cs.blocks) AS blocks
FROM caches c
LEFT JOIN master_index_stats ms ON ms.master_index_id = c.id
LEFT JOIN crc_table_stats cs ON cs.crc_table_id = c.id;
