-- @formatter:off
CREATE MATERIALIZED VIEW index_stats_new (
    scope_id,
    archive_id,
    container_id,
    valid_groups,
    groups,
    valid_keys,
    keys,
    size,
    blocks
) AS
SELECT
    s.id AS scope_id,
    g.group_id AS archive_id,
    i.container_id,
    COUNT(*) FILTER (WHERE c.id IS NOT NULL) AS valid_groups,
    COUNT(*) AS groups,
    COUNT(*) FILTER (WHERE c.encrypted AND (c.key_id IS NOT NULL OR c.empty_loc)) AS valid_keys,
    COUNT(*) FILTER (WHERE c.encrypted) AS keys,
    SUM(length(c.data) + 2) FILTER (WHERE c.id IS NOT NULL) AS size,
    SUM(group_blocks(ig.group_id, length(c.data) + 2)) FILTER (WHERE c.id IS NOT NULL) AS blocks
FROM scopes s
CROSS JOIN indexes i
JOIN groups g ON g.scope_id = s.id AND g.container_id = i.container_id AND g.archive_id = 255 AND
    NOT g.version_truncated AND g.version = i.version
JOIN index_groups ig ON ig.container_id = i.container_id
LEFT JOIN resolve_group(s.id, g.group_id::uint1, ig.group_id, ig.crc32, ig.version) c ON TRUE
GROUP BY s.id, g.group_id, i.container_id
WITH NO DATA;

CREATE UNIQUE INDEX ON index_stats_new (scope_id, archive_id, container_id);

ALTER MATERIALIZED VIEW index_stats RENAME TO index_stats_old;
ALTER INDEX index_stats_scope_id_archive_id_container_id_idx RENAME TO index_stats_old_scope_id_archive_id_container_id_idx;

ALTER MATERIALIZED VIEW index_stats_new RENAME TO index_stats;
ALTER INDEX index_stats_new_scope_id_archive_id_container_id_idx RENAME TO index_stats_scope_id_archive_id_container_id_idx;

CREATE MATERIALIZED VIEW master_index_stats_new (
    scope_id,
    master_index_id,
    valid_indexes,
    indexes,
    valid_groups,
    groups,
    valid_keys,
    keys,
    size,
    blocks
) AS
SELECT
    sc.id,
    m.id,
    COUNT(*) FILTER (WHERE c.id IS NOT NULL OR (a.version = 0 AND a.crc32 = 0)) AS valid_indexes,
    COUNT(*) FILTER (WHERE a.master_index_id IS NOT NULL) AS indexes,
    SUM(COALESCE(s.valid_groups, 0)) AS valid_groups,
    SUM(COALESCE(s.groups, 0)) AS groups,
    SUM(COALESCE(s.valid_keys, 0)) AS valid_keys,
    SUM(COALESCE(s.keys, 0)) AS keys,
    SUM(COALESCE(s.size, 0)) + SUM(COALESCE(length(c.data), 0)) AS size,
    SUM(COALESCE(s.blocks, 0)) + SUM(COALESCE(group_blocks(a.archive_id, length(c.data)), 0)) AS blocks
FROM scopes sc
CROSS JOIN master_indexes m
LEFT JOIN master_index_archives a ON a.master_index_id = m.id
LEFT JOIN resolve_index(sc.id, a.archive_id, a.crc32, a.version) c ON TRUE
LEFT JOIN index_stats s ON s.scope_id = sc.id AND s.archive_id = a.archive_id AND s.container_id = c.id
GROUP BY sc.id, m.id
WITH NO DATA;

CREATE UNIQUE INDEX ON master_index_stats_new (scope_id, master_index_id);

ALTER MATERIALIZED VIEW master_index_stats RENAME TO master_index_stats_old;
ALTER INDEX master_index_stats_scope_id_master_index_id_idx RENAME TO master_index_stats_old_scope_id_master_index_id_idx;

ALTER MATERIALIZED VIEW master_index_stats_new RENAME TO master_index_stats;
ALTER INDEX master_index_stats_new_scope_id_master_index_id_idx RENAME TO master_index_stats_scope_id_master_index_id_idx;

CREATE OR REPLACE VIEW cache_stats AS
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

DROP MATERIALIZED VIEW master_index_stats_old;
DROP MATERIALIZED VIEW index_stats_old;
