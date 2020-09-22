-- @formatter:off
CREATE EXTENSION uint;

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
    crc32 INT NOT NULL,
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
    truncated_version uint2 NOT NULL,
    PRIMARY KEY (archive_id, group_id, container_id, truncated_version)
);

CREATE TABLE indexes (
    container_id BIGINT PRIMARY KEY NOT NULL REFERENCES containers (id),
    version INTEGER NOT NULL
);

CREATE TABLE index_groups (
    container_id BIGINT NOT NULL REFERENCES indexes (container_id),
    group_id INTEGER NOT NULL,
    crc32 INTEGER NOT NULL,
    whirlpool BYTEA NULL,
    version INTEGER NOT NULL,
    name_hash INTEGER NULL,
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

CREATE TABLE caches (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    -- This doesn't correspond to a hash used by the client - it was just
    -- convenient to re-use Whirlpool given we already use it elsewhere in the
    -- codebase.
    --
    -- It is a hash over the whirlpool hashes of a cache's Js5Indexes, used to
    -- make it easier to identify an individual cache row in a relational
    -- database.
    whirlpool BYTEA UNIQUE NOT NULL
);

CREATE TABLE cache_indexes (
    cache_id BIGINT NOT NULL REFERENCES caches (id),
    archive_id uint1 NOT NULL,
    container_id BIGINT NOT NULL REFERENCES indexes (container_id),
    PRIMARY KEY (cache_id, archive_id)
);

CREATE TABLE names (
    hash INTEGER NOT NULL,
    name TEXT PRIMARY KEY NOT NULL
);

CREATE UNIQUE INDEX ON names (hash, name);
-- @formatter:on
