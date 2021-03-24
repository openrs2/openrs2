-- @formatter:off
CREATE EXTENSION IF NOT EXISTS uint;

CREATE TABLE games (
    id SERIAL PRIMARY KEY NOT NULL,
    name TEXT UNIQUE NOT NULL,
    hostname TEXT NULL,
    port uint2 NULL,
    build INTEGER NULL
);

INSERT INTO games (name, hostname, port, build)
VALUES
    ('runescape', NULL, NULL, NULL),
    ('oldschool', 'oldschool1.runescape.com', 43594, 193);

CREATE TYPE xtea_key AS (
    k0 INTEGER,
    k1 INTEGER,
    k2 INTEGER,
    k3 INTEGER
);

CREATE TABLE keys (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    key xtea_key UNIQUE NOT NULL CHECK ((key).k0 <> 0 OR (key).k1 <> 0 OR (key).k2 <> 0 OR (key).k3 <> 0)
);

CREATE TABLE keysets (
    url TEXT PRIMARY KEY NOT NULL
);

CREATE TABLE containers (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    crc32 INTEGER NOT NULL,
    whirlpool BYTEA UNIQUE NOT NULL,
    data BYTEA NOT NULL,
    uncompressed_length INTEGER NULL,
    uncompressed_crc32 INTEGER NULL,
    encrypted BOOLEAN NOT NULL,
    empty_loc BOOLEAN NULL,
    key_id BIGINT NULL REFERENCES keys (id)
);

CREATE INDEX ON containers USING HASH (crc32);
CREATE INDEX ON containers (id) WHERE encrypted AND key_id IS NULL;

CREATE TABLE brute_force_iterator (
    last_container_id BIGINT NULL,
    last_key_id BIGINT NULL
);

CREATE UNIQUE INDEX ON brute_force_iterator ((TRUE));

INSERT INTO brute_force_iterator (last_container_id, last_key_id)
VALUES (NULL, NULL);

CREATE TABLE groups (
    archive_id uint1 NOT NULL,
    group_id INTEGER NOT NULL,
    version INTEGER NOT NULL,
    version_truncated BOOLEAN NOT NULL,
    container_id BIGINT NOT NULL REFERENCES containers (id),
    PRIMARY KEY (archive_id, group_id, version, version_truncated, container_id)
);

CREATE TABLE indexes (
    container_id BIGINT PRIMARY KEY NOT NULL REFERENCES containers (id),
    protocol uint1 NOT NULL,
    version INTEGER NOT NULL,
    has_names BOOLEAN NOT NULL,
    has_digests BOOLEAN NOT NULL,
    has_lengths BOOLEAN NOT NULL,
    has_uncompressed_checksums BOOLEAN NOT NULL
);

CREATE TABLE index_groups (
    container_id BIGINT NOT NULL REFERENCES indexes (container_id),
    group_id INTEGER NOT NULL,
    crc32 INTEGER NOT NULL,
    whirlpool BYTEA NULL,
    version INTEGER NOT NULL,
    name_hash INTEGER NULL,
    length INTEGER NULL,
    uncompressed_length INTEGER NULL,
    uncompressed_crc32 INTEGER NULL,
    PRIMARY KEY (container_id, group_id)
);

CREATE TABLE index_files (
    container_id BIGINT NOT NULL,
    group_id INTEGER NOT NULL,
    file_id INTEGER NOT NULL,
    name_hash INTEGER NULL,
    PRIMARY KEY (container_id, group_id, file_id),
    FOREIGN KEY (container_id, group_id) REFERENCES index_groups (container_id, group_id)
);

CREATE TYPE master_index_format AS ENUM (
    'original',
    'versioned',
    'digests',
    'lengths'
);

CREATE TABLE master_indexes (
    id SERIAL PRIMARY KEY NOT NULL,
    container_id BIGINT NOT NULL REFERENCES containers (id),
    format master_index_format NOT NULL,
    UNIQUE (container_id, format)
);

ALTER TABLE games ADD COLUMN last_master_index_id INT NULL REFERENCES master_indexes (id);

CREATE TABLE master_index_archives (
    master_index_id INTEGER NOT NULL REFERENCES master_indexes (id),
    archive_id uint1 NOT NULL,
    crc32 INTEGER NOT NULL,
    whirlpool BYTEA NULL,
    version INTEGER NOT NULL,
    groups INTEGER NULL,
    total_uncompressed_length INTEGER NULL,
    PRIMARY KEY (master_index_id, archive_id)
);

CREATE TYPE source_type AS ENUM (
    'disk',
    'js5remote'
);

CREATE TABLE sources (
    id SERIAL PRIMARY KEY NOT NULL,
    type source_type NOT NULL,
    master_index_id INTEGER NOT NULL REFERENCES master_indexes (id),
    game_id INTEGER NOT NULL REFERENCES games (id),
    build INTEGER NULL,
    timestamp TIMESTAMPTZ NULL,
    name TEXT NULL,
    description TEXT NULL,
    url TEXT NULL
);

CREATE INDEX ON sources (master_index_id);
CREATE UNIQUE INDEX ON sources (master_index_id, game_id, build) WHERE type = 'js5remote';

CREATE TABLE source_groups (
    source_id INTEGER NOT NULL REFERENCES sources (id),
    archive_id uint1 NOT NULL,
    group_id INTEGER NOT NULL,
    version INTEGER NOT NULL,
    version_truncated BOOLEAN NOT NULL,
    container_id BIGINT NOT NULL REFERENCES containers (id),
    PRIMARY KEY (source_id, archive_id, group_id),
    FOREIGN KEY (archive_id, group_id, version, version_truncated, container_id) REFERENCES groups (archive_id, group_id, version, version_truncated, container_id)
);

CREATE INDEX ON source_groups (archive_id, group_id, version, version_truncated, container_id);

CREATE TABLE names (
    hash INTEGER NOT NULL,
    name TEXT PRIMARY KEY NOT NULL
);

CREATE UNIQUE INDEX ON names (hash, name);

CREATE FUNCTION resolve_index(_archive_id uint1, _crc32 INTEGER, _version INTEGER) RETURNS SETOF containers AS $$
    SELECT c.*
    FROM groups g
    JOIN containers c ON c.id = g.container_id
    JOIN indexes i ON i.container_id = c.id
    WHERE g.archive_id = 255 AND g.group_id = _archive_id::INTEGER AND c.crc32 = _crc32 AND g.version = _version AND
        NOT g.version_truncated AND i.version = _version
    ORDER BY c.id ASC
    LIMIT 1;
$$ LANGUAGE SQL STABLE PARALLEL SAFE ROWS 1;

CREATE FUNCTION resolve_group(_archive_id uint1, _group_id INTEGER, _crc32 INTEGER, _version INTEGER) RETURNS SETOF containers AS $$
    SELECT c.*
    FROM groups g
    JOIN containers c ON c.id = g.container_id
    WHERE g.archive_id = _archive_id AND g.group_id = _group_id AND c.crc32 = _crc32 AND g.version = _version & 65535
    ORDER BY g.version_truncated ASC, c.id ASC
    LIMIT 1;
$$ LANGUAGE SQL STABLE PARALLEL SAFE ROWS 1;

CREATE VIEW resolved_indexes AS
SELECT m.id AS master_index_id, a.archive_id, c.data, c.id AS container_id
FROM master_indexes m
JOIN master_index_archives a ON a.master_index_id = m.id
JOIN resolve_index(a.archive_id, a.crc32, a.version) c ON TRUE;

CREATE VIEW resolved_groups (master_index_id, archive_id, group_id, name_hash, version, data, key_id) AS
WITH i AS NOT MATERIALIZED (
    SELECT master_index_id, archive_id, data, container_id
    FROM resolved_indexes
)
SELECT i.master_index_id, 255::uint1, i.archive_id::INTEGER, NULL, NULL, i.data, NULL
FROM i
UNION ALL
SELECT i.master_index_id, i.archive_id, ig.group_id, ig.name_hash, ig.version, c.data, c.key_id
FROM i
JOIN index_groups ig ON ig.container_id = i.container_id
JOIN resolve_group(i.archive_id, ig.group_id, ig.crc32, ig.version) c ON TRUE;

CREATE MATERIALIZED VIEW master_index_stats (
    master_index_id,
    valid_indexes,
    indexes,
    valid_groups,
    groups,
    valid_keys,
    keys
) AS
SELECT
    m.id,
    COALESCE(a.valid_indexes, 0),
    COALESCE(a.indexes, 0),
    COALESCE(g.valid_groups, 0),
    COALESCE(g.groups, 0),
    COALESCE(g.valid_keys, 0),
    COALESCE(g.keys, 0)
FROM master_indexes m
LEFT JOIN (
    SELECT
        a.master_index_id,
        COUNT(*) FILTER (WHERE c.id IS NOT NULL OR (a.version = 0 AND a.crc32 = 0)) AS valid_indexes,
        COUNT(*) AS indexes
    FROM master_index_archives a
    LEFT JOIN resolve_index(a.archive_id, a.crc32, a.version) c ON TRUE
    GROUP BY a.master_index_id
) a ON a.master_index_id = m.id
LEFT JOIN (
    SELECT
        i.master_index_id,
        COUNT(*) FILTER (WHERE c.id IS NOT NULL) AS valid_groups,
        COUNT(*) AS groups,
        COUNT(*) FILTER (WHERE c.key_id IS NOT NULL) AS valid_keys,
        COUNT(*) FILTER (WHERE c.encrypted) AS keys
    FROM resolved_indexes i
    JOIN index_groups ig ON ig.container_id = i.container_id
    LEFT JOIN resolve_group(i.archive_id, ig.group_id, ig.crc32, ig.version) c ON TRUE
    LEFT JOIN keys k ON k.id = c.key_id
    GROUP BY i.master_index_id
) g ON g.master_index_id = m.id;

CREATE UNIQUE INDEX ON master_index_stats (master_index_id);

CREATE VIEW collisions (archive_id, group_id, crc32, truncated_version, versions, containers) AS
SELECT
    g.archive_id,
    g.group_id,
    c.crc32,
    g.version & 65535 AS truncated_version,
    array_agg(DISTINCT g.version ORDER BY g.version ASC),
    array_agg(DISTINCT c.id ORDER BY c.id ASC)
FROM groups g
JOIN containers c ON c.id = g.container_id
GROUP BY g.archive_id, g.group_id, c.crc32, truncated_version
HAVING COUNT(DISTINCT c.id) > 1;
-- @formatter:on
