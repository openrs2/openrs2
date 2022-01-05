-- @formatter:off
CREATE TYPE key_source AS ENUM (
    'api',
    'disk',
    'openosrs',
    'polar',
    'runelite'
);

CREATE TABLE key_sources (
    key_id BIGINT NOT NULL REFERENCES keys (id),
    source key_source NOT NULL,
    first_seen TIMESTAMPTZ NOT NULL,
    last_seen TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (key_id, source)
);

CREATE TABLE key_queue (
    key xtea_key NOT NULL,
    source key_source NOT NULL,
    first_seen TIMESTAMPTZ NOT NULL,
    last_seen TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (key, source)
);
