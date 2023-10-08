-- @formatter:off

CREATE TABLE artifact_sources (
    id SERIAL PRIMARY KEY NOT NULL,
    blob_id BIGINT NOT NULL REFERENCES artifacts (blob_id),
    name TEXT NULL,
    description TEXT NULL,
    url TEXT NULL
);

CREATE INDEX ON artifact_sources (blob_id);
