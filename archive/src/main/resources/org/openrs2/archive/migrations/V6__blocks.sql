-- @formatter:off
CREATE FUNCTION group_blocks(group_id INTEGER, len INTEGER) RETURNS INTEGER AS $$
    SELECT CASE
        WHEN len = 0 THEN 1
        WHEN group_id >= 65536 THEN (len + 509) / 510
        ELSE (len + 511) / 512
    END;
$$ LANGUAGE SQL IMMUTABLE PARALLEL SAFE;

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
    COALESCE(a.valid_indexes, 0),
    COALESCE(a.indexes, 0),
    COALESCE(g.valid_groups, 0),
    COALESCE(g.groups, 0),
    COALESCE(g.valid_keys, 0),
    COALESCE(g.keys, 0),
    COALESCE(a.size, 0) + COALESCE(g.size, 0),
    COALESCE(a.blocks, 0) + COALESCE(g.blocks, 0)
FROM master_indexes m
LEFT JOIN (
    SELECT
        a.master_index_id,
        COUNT(*) FILTER (WHERE c.id IS NOT NULL OR (a.version = 0 AND a.crc32 = 0)) AS valid_indexes,
        COUNT(*) AS indexes,
        SUM(length(c.data)) FILTER (WHERE c.id IS NOT NULL) AS size,
        SUM(group_blocks(a.archive_id, length(c.data))) FILTER (WHERE c.id IS NOT NULL) AS blocks
    FROM master_index_archives a
    LEFT JOIN resolve_index(a.archive_id, a.crc32, a.version) c ON TRUE
    GROUP BY a.master_index_id
) a ON a.master_index_id = m.id
LEFT JOIN (
    SELECT
        i.master_index_id,
        COUNT(*) FILTER (WHERE c.id IS NOT NULL) AS valid_groups,
        COUNT(*) AS groups,
        COUNT(*) FILTER (WHERE c.encrypted AND (c.key_id IS NOT NULL OR c.empty_loc)) AS valid_keys,
        COUNT(*) FILTER (WHERE c.encrypted) AS keys,
        SUM(length(c.data)) FILTER (WHERE c.id IS NOT NULL) AS size,
        SUM(group_blocks(ig.group_id, length(c.data))) FILTER (WHERE c.id IS NOT NULL) AS blocks
    FROM resolved_indexes i
    JOIN index_groups ig ON ig.container_id = i.container_id
    LEFT JOIN resolve_group(i.archive_id, ig.group_id, ig.crc32, ig.version) c ON TRUE
    LEFT JOIN keys k ON k.id = c.key_id
    GROUP BY i.master_index_id
) g ON g.master_index_id = m.id
WITH NO DATA;

CREATE UNIQUE INDEX ON master_index_stats_new (master_index_id);

ALTER MATERIALIZED VIEW master_index_stats RENAME TO master_index_stats_old;
ALTER INDEX master_index_stats_master_index_id_idx RENAME TO master_index_stats_old_master_index_id_idx;

ALTER MATERIALIZED VIEW master_index_stats_new RENAME TO master_index_stats;
ALTER INDEX master_index_stats_new_master_index_id_idx RENAME TO master_index_stats_master_index_id_idx;

DROP MATERIALIZED VIEW master_index_stats_old;
