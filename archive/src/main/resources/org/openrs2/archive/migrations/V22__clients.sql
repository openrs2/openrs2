-- @formatter:off

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE blobs ADD COLUMN sha1 BYTEA NULL;

UPDATE blobs SET sha1 = digest(data, 'sha1');

ALTER TABLE blobs ALTER COLUMN sha1 SET NOT NULL;

-- not UNIQUE as SHA-1 collisions are possible
CREATE INDEX ON blobs USING HASH (sha1);

INSERT INTO scopes (name) VALUES ('shared');
INSERT INTO games (name, scope_id) VALUES ('shared', (SELECT id FROM scopes WHERE name = 'shared'));

INSERT INTO scopes (name) VALUES ('classic');
INSERT INTO games (name, scope_id) VALUES ('classic', (SELECT id FROM scopes WHERE name = 'classic'));

INSERT INTO scopes (name) VALUES ('mapview');
INSERT INTO games (name, scope_id) VALUES ('mapview', (SELECT id FROM scopes WHERE name = 'mapview'));

CREATE TYPE artifact_type AS ENUM (
    'browsercontrol',
    'client',
    'client_gl',
    'gluegen_rt',
    'jaggl',
    'jaggl_dri',
    'jagmisc',
    'jogl',
    'jogl_awt',
    'loader',
    'loader_gl',
    'unpackclass'
);

CREATE TYPE artifact_format AS ENUM (
    'cab',
    'jar',
    'native',
    'pack200',
    'packclass'
);

CREATE TYPE os AS ENUM (
    'independent',
    'windows',
    'macos',
    'linux',
    'solaris'
);

CREATE TYPE arch AS ENUM (
    'independent',
    'universal',
    'x86',
    'amd64',
    'powerpc',
    'sparc',
    'sparcv9'
);

CREATE TYPE jvm AS ENUM (
    'independent',
    'sun',
    'microsoft'
);

CREATE TABLE artifacts (
    blob_id BIGINT PRIMARY KEY NOT NULL REFERENCES blobs (id),
    game_id INTEGER NOT NULL REFERENCES games (id),
    environment_id INTEGER NOT NULL REFERENCES environments (id),
    build_major INTEGER NULL,
    build_minor INTEGER NULL,
    timestamp TIMESTAMPTZ NULL,
    type artifact_type NOT NULL,
    format artifact_format NOT NULL,
    os os NOT NULL,
    arch arch NOT NULL,
    jvm jvm NOT NULL
);

CREATE TABLE artifact_links (
    blob_id BIGINT NOT NULL REFERENCES artifacts (blob_id),
    type artifact_type NOT NULL,
    format artifact_format NOT NULL,
    os os NOT NULL,
    arch arch NOT NULL,
    jvm jvm NOT NULL,
    sha1 BYTEA NOT NULL,
    crc32 INTEGER NULL,
    size INTEGER NULL,
    PRIMARY KEY (blob_id, type, format, os, arch, jvm)
);
