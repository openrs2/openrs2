-- @formatter:off
CREATE TABLE scopes (
    id SERIAL PRIMARY KEY NOT NULL,
    name TEXT UNIQUE NOT NULL
);

INSERT INTO scopes (name) VALUES ('runescape');

ALTER TABLE games
    ADD COLUMN scope_id INTEGER DEFAULT 1 NOT NULL REFERENCES scopes (id);

ALTER TABLE games
    ALTER COLUMN scope_id DROP DEFAULT;

-- XXX(gpe): I don't think we can easily replace this as the source_groups
-- table doesn't contain a scope_id directly - only indirectly via the sources
-- and games tables.
ALTER TABLE source_groups
    DROP CONSTRAINT source_groups_archive_id_group_id_version_version_truncate_fkey;

ALTER TABLE groups
    ADD COLUMN scope_id INTEGER DEFAULT 1 NOT NULL REFERENCES scopes (id),
    DROP CONSTRAINT groups_pkey,
    ADD PRIMARY KEY (scope_id, archive_id, group_id, version, version_truncated, container_id);

ALTER TABLE groups
    ALTER COLUMN scope_id DROP DEFAULT;

CREATE FUNCTION resolve_index(_scope_id INTEGER, _archive_id uint1, _crc32 INTEGER, _version INTEGER) RETURNS SETOF containers AS $$
    SELECT c.*
    FROM groups g
    JOIN containers c ON c.id = g.container_id
    JOIN indexes i ON i.container_id = c.id
    WHERE g.scope_id = _scope_id AND g.archive_id = 255 AND g.group_id = _archive_id::INTEGER AND c.crc32 = _crc32 AND
        g.version = _version AND NOT g.version_truncated AND i.version = _version
    ORDER BY c.id ASC
    LIMIT 1;
$$ LANGUAGE SQL STABLE PARALLEL SAFE ROWS 1;

CREATE FUNCTION resolve_group(_scope_id INTEGER, _archive_id uint1, _group_id INTEGER, _crc32 INTEGER, _version INTEGER) RETURNS SETOF containers AS $$
    SELECT c.*
    FROM groups g
    JOIN containers c ON c.id = g.container_id
    WHERE g.scope_id = _scope_id AND g.archive_id = _archive_id AND g.group_id = _group_id AND c.crc32 = _crc32 AND (
        (g.version = _version AND NOT g.version_truncated) OR
        (g.version = _version & 65535 AND g.version_truncated)
    )
    ORDER BY g.version_truncated ASC, c.id ASC
    LIMIT 1;
$$ LANGUAGE SQL STABLE PARALLEL SAFE ROWS 1;

DROP VIEW resolved_groups;
DROP VIEW resolved_indexes;

CREATE VIEW resolved_indexes AS
SELECT s.id AS scope_id, m.id AS master_index_id, a.archive_id, c.data, c.id AS container_id
FROM scopes s
CROSS JOIN master_indexes m
JOIN master_index_archives a ON a.master_index_id = m.id
JOIN resolve_index(s.id, a.archive_id, a.crc32, a.version) c ON TRUE;

CREATE VIEW resolved_groups (scope_id, master_index_id, archive_id, group_id, name_hash, version, data, encrypted, empty_loc, key_id) AS
WITH i AS NOT MATERIALIZED (
    SELECT scope_id, master_index_id, archive_id, data, container_id
    FROM resolved_indexes
)
SELECT i.scope_id, i.master_index_id, 255::uint1, i.archive_id::INTEGER, NULL, NULL, i.data, FALSE, FALSE, NULL
FROM i
UNION ALL
SELECT i.scope_id, i.master_index_id, i.archive_id, ig.group_id, ig.name_hash, ig.version, c.data, c.encrypted, c.empty_loc, c.key_id
FROM i
JOIN index_groups ig ON ig.container_id = i.container_id
JOIN resolve_group(i.scope_id, i.archive_id, ig.group_id, ig.crc32, ig.version) c ON TRUE;

DROP VIEW colliding_groups;

CREATE VIEW colliding_groups (scope_id, archive_id, group_id, crc32, truncated_version, versions, containers) AS
SELECT
    g.scope_id,
    g.archive_id,
    g.group_id,
    c.crc32,
    g.version & 65535 AS truncated_version,
    array_agg(DISTINCT g.version ORDER BY g.version ASC),
    array_agg(DISTINCT c.id ORDER BY c.id ASC)
FROM groups g
JOIN containers c ON c.id = g.container_id
GROUP BY g.scope_id, g.archive_id, g.group_id, c.crc32, truncated_version
HAVING COUNT(DISTINCT c.id) > 1;

DROP VIEW cache_stats;
DROP MATERIALIZED VIEW master_index_stats;
DROP MATERIALIZED VIEW index_stats;

CREATE MATERIALIZED VIEW index_stats (
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
JOIN groups g ON g.container_id = i.container_id AND g.archive_id = 255 AND NOT g.version_truncated AND
    g.version = i.version
JOIN index_groups ig ON ig.container_id = i.container_id
LEFT JOIN resolve_group(s.id, g.group_id::uint1, ig.group_id, ig.crc32, ig.version) c ON TRUE
GROUP BY s.id, g.group_id, i.container_id;

CREATE UNIQUE INDEX ON index_stats (scope_id, archive_id, container_id);

CREATE MATERIALIZED VIEW master_index_stats (
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
GROUP BY sc.id, m.id;

CREATE UNIQUE INDEX ON master_index_stats (scope_id, master_index_id);

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

DROP FUNCTION resolve_group(_archive_id uint1, _group_id INTEGER, _crc32 INTEGER, _version INTEGER);
DROP FUNCTION resolve_index(_archive_id uint1, _crc32 INTEGER, _version INTEGER);
