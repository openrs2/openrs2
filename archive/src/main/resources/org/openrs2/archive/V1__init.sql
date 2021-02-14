-- @formatter:off
CREATE EXTENSION uint;

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

CREATE TABLE containers (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    crc32 INTEGER NOT NULL,
    whirlpool BYTEA UNIQUE NOT NULL,
    data BYTEA NOT NULL,
    uncompressed_length INTEGER NULL,
    uncompressed_crc32 INTEGER NULL,
    encrypted BOOLEAN NOT NULL,
    key_id BIGINT NULL REFERENCES keys (id)
);

CREATE INDEX ON containers USING HASH (crc32);

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
    container_id BIGINT NOT NULL REFERENCES containers (id),
    version INTEGER NOT NULL,
    version_truncated BOOLEAN NOT NULL,
    PRIMARY KEY (archive_id, group_id, container_id, version, version_truncated)
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
    game_id INTEGER NOT NULL REFERENCES games (id),
    build INTEGER NULL,
    timestamp TIMESTAMPTZ NULL,
    name TEXT NULL,
    description TEXT NULL,
    UNIQUE (container_id, format)
);

ALTER TABLE games ADD COLUMN last_master_index_id INT NULL REFERENCES master_indexes (id);

CREATE TABLE master_index_archives (
    master_index_id INTEGER NOT NULL REFERENCES master_indexes (id),
    archive_id uint1 NOT NULL,
    crc32 INTEGER NOT NULL,
    whirlpool BYTEA NULL,
    version INTEGER NOT NULL,
    PRIMARY KEY (master_index_id, archive_id)
);

CREATE TABLE names (
    hash INTEGER NOT NULL,
    name TEXT PRIMARY KEY NOT NULL
);

CREATE UNIQUE INDEX ON names (hash, name);

CREATE VIEW master_index_valid_indexes AS
SELECT m.id AS master_index_id, a.archive_id, c.data, g.container_id
FROM master_indexes m
JOIN master_index_archives a ON a.master_index_id = m.id
JOIN groups g ON g.archive_id = 255 AND g.group_id = a.archive_id::INTEGER AND
    g.version = a.version AND NOT g.version_truncated
JOIN containers c ON c.id = g.container_id AND c.crc32 = a.crc32
JOIN indexes i ON i.container_id = g.container_id AND i.version = a.version;

-- TODO(gpe): think about what to do if there is a collision
CREATE VIEW master_index_valid_groups (master_index_id, archive_id, group_id, name_hash, version, data, key_id) AS
WITH i AS NOT MATERIALIZED (
    SELECT master_index_id, archive_id, data, container_id
    FROM master_index_valid_indexes
)
SELECT i.master_index_id, 255::uint1, i.archive_id::INTEGER, NULL, NULL, i.data, NULL
FROM i
UNION ALL
SELECT i.master_index_id, i.archive_id, ig.group_id, ig.name_hash, ig.version, c.data, c.key_id
FROM i
JOIN index_groups ig ON ig.container_id = i.container_id
JOIN groups g ON g.archive_id = i.archive_id AND g.group_id = ig.group_id AND (
    (g.version = ig.version AND NOT g.version_truncated) OR
    (g.version = ig.version & 65535 AND g.version_truncated)
)
JOIN containers c ON c.id = g.container_id AND c.crc32 = ig.crc32;
-- @formatter:on
