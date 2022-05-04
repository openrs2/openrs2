-- @formatter:off
DROP VIEW cache_stats;
DROP MATERIALIZED VIEW crc_table_stats;
DROP MATERIALIZED VIEW version_list_stats;

CREATE MATERIALIZED VIEW version_list_stats AS
SELECT
    v.blob_id,
    vf.index_id,
    COUNT(*) FILTER (WHERE b.id IS NOT NULL) AS valid_files,
    COUNT(*) AS files,
    SUM(length(b.data) + 2) FILTER (WHERE b.id IS NOT NULL) AS size,
    SUM(group_blocks(vf.file_id, length(b.data) + 2)) AS blocks
FROM version_lists v
JOIN version_list_files vf ON vf.blob_id = v.blob_id
LEFT JOIN resolve_file(vf.index_id, vf.file_id, vf.version, vf.crc32) b ON TRUE
GROUP BY v.blob_id, vf.index_id;

CREATE UNIQUE INDEX ON version_list_stats (blob_id, index_id);

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
GROUP BY c.id;

CREATE UNIQUE INDEX ON crc_table_stats (crc_table_id);

CREATE VIEW cache_stats AS
SELECT
    s.id AS scope_id,
    c.id AS cache_id,
    COALESCE(ms.valid_indexes, cs.valid_archives) AS valid_indexes,
    COALESCE(ms.indexes, cs.archives) AS indexes,
    COALESCE(ms.valid_groups, cs.valid_files) AS valid_groups,
    COALESCE(ms.groups, cs.files) AS groups,
    COALESCE(ms.valid_keys, 0) AS valid_keys,
    COALESCE(ms.keys, 0) AS keys,
    COALESCE(ms.size, cs.size) AS size,
    COALESCE(ms.blocks, cs.blocks) AS blocks
FROM scopes s
CROSS JOIN caches c
LEFT JOIN master_index_stats ms ON ms.scope_id = s.id AND ms.master_index_id = c.id
LEFT JOIN crc_table_stats cs ON s.name = 'runescape' AND cs.crc_table_id = c.id;
