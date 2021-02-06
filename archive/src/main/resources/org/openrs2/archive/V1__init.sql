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
    container_id BIGINT PRIMARY KEY NOT NULL REFERENCES containers (id)
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

CREATE TABLE master_indexes (
    container_id BIGINT PRIMARY KEY NOT NULL REFERENCES containers (id),
    game_id INTEGER NOT NULL REFERENCES games (id),
    build INTEGER NULL,
    timestamp TIMESTAMPTZ NULL,
    name TEXT NULL,
    description TEXT NULL
);

CREATE TABLE master_index_archives (
    container_id BIGINT NOT NULL REFERENCES master_indexes (container_id),
    archive_id uint1 NOT NULL,
    crc32 INTEGER NOT NULL,
    whirlpool BYTEA NULL,
    version INTEGER NOT NULL,
    PRIMARY KEY (container_id, archive_id)
);

CREATE TABLE names (
    hash INTEGER NOT NULL,
    name TEXT PRIMARY KEY NOT NULL
);

CREATE UNIQUE INDEX ON names (hash, name);
-- @formatter:on
