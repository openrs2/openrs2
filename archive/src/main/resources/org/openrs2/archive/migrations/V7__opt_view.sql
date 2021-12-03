-- @formatter:off
CREATE MATERIALIZED VIEW index_stats (
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
    g.group_id AS archive_id,
    i.container_id,
    COUNT(*) FILTER (WHERE c.id IS NOT NULL) AS valid_groups,
    COUNT(*) AS groups,
    COUNT(*) FILTER (WHERE c.encrypted AND (c.key_id IS NOT NULL OR c.empty_loc)) AS valid_keys,
    COUNT(*) FILTER (WHERE c.encrypted) AS keys,
    SUM(length(c.data)) FILTER (WHERE c.id IS NOT NULL) AS size,
    SUM(group_blocks(ig.group_id, length(c.data))) FILTER (WHERE c.id IS NOT NULL) AS blocks
FROM indexes i
JOIN groups g ON g.container_id = i.container_id AND g.archive_id = 255 AND NOT g.version_truncated AND
    g.version = i.version
JOIN index_groups ig ON ig.container_id = i.container_id
LEFT JOIN resolve_group(g.group_id::uint1, ig.group_id, ig.crc32, ig.version) c ON TRUE
GROUP BY g.group_id, i.container_id;

CREATE UNIQUE INDEX ON index_stats (container_id);

CREATE MATERIALIZED VIEW master_index_stats_new (
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
    m.id,
    COUNT(*) FILTER (WHERE c.id IS NOT NULL OR (a.version = 0 AND a.crc32 = 0)) AS valid_indexes,
    COUNT(*) FILTER (WHERE a.master_index_id IS NOT NULL) AS indexes,
    SUM(COALESCE(s.valid_groups, 0)) AS valid_groups,
    SUM(COALESCE(s.groups, 0)) AS groups,
    SUM(COALESCE(s.valid_keys, 0)) AS valid_keys,
    SUM(COALESCE(s.keys, 0)) AS keys,
    SUM(COALESCE(s.size, 0)) + SUM(COALESCE(length(c.data), 0)) AS size,
    SUM(COALESCE(s.blocks, 0)) + SUM(COALESCE(group_blocks(a.archive_id, length(c.data)), 0)) AS blocks
FROM master_indexes m
LEFT JOIN master_index_archives a ON a.master_index_id = m.id
LEFT JOIN resolve_index(a.archive_id, a.crc32, a.version) c ON TRUE
LEFT JOIN index_stats s ON s.archive_id = a.archive_id AND s.container_id = c.id
GROUP BY m.id;

CREATE UNIQUE INDEX ON master_index_stats_new (master_index_id);

ALTER MATERIALIZED VIEW master_index_stats RENAME TO master_index_stats_old;
ALTER INDEX master_index_stats_master_index_id_idx RENAME TO master_index_stats_old_master_index_id_idx;

ALTER MATERIALIZED VIEW master_index_stats_new RENAME TO master_index_stats;
ALTER INDEX master_index_stats_new_master_index_id_idx RENAME TO master_index_stats_master_index_id_idx;

DROP MATERIALIZED VIEW master_index_stats_old;
