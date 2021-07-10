-- @formatter:off
CREATE TABLE updates (
    master_index_id INTEGER NOT NULL REFERENCES master_indexes (id),
    url TEXT NOT NULL,
    PRIMARY KEY (master_index_id, url)
);
-- @formatter:on
