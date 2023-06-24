-- @formatter:off

ALTER TABLE sources
    ALTER COLUMN cache_id DROP NOT NULL,
    ALTER COLUMN game_id DROP NOT NULL;

CREATE UNIQUE INDEX ON sources (type) WHERE type = 'cross_pollination';
