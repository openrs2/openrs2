-- @formatter:off
CREATE TABLE environments (
    id INTEGER NOT NULL PRIMARY KEY,
    name TEXT NOT NULL
);

INSERT INTO environments (id, name)
VALUES
    (1, 'live'),
    (2, 'beta');

CREATE TABLE languages (
    -- Not SERIAL as these IDs are allocated by Jagex, not us.
    id INTEGER NOT NULL PRIMARY KEY,
    iso_code TEXT NOT NULL
);

INSERT INTO languages (id, iso_code)
VALUES
    (0, 'en'),
    (1, 'de'),
    (2, 'fr'),
    (3, 'pt');

ALTER TABLE games RENAME TO game_variants;

ALTER INDEX games_pkey RENAME TO game_variants_pkey;
ALTER INDEX games_name_key RENAME TO game_variants_name_key;

ALTER SEQUENCE games_id_seq RENAME TO game_variants_id_seq;

CREATE TABLE games (
    id SERIAL NOT NULL PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

INSERT INTO games (id, name)
SELECT id, name
FROM game_variants;

SELECT setval('games_id_seq', MAX(id)) FROM game_variants;

ALTER TABLE game_variants
    ADD COLUMN game_id INT NULL REFERENCES games (id),
    ADD COLUMN environment_id INT NOT NULL DEFAULT 1 REFERENCES environments (id),
    ADD COLUMN language_id INT NOT NULL DEFAULT 0 REFERENCES languages (id);

UPDATE game_variants v
SET game_id = g.id, environment_id = 1, language_id = 0
FROM games g
WHERE g.name = v.name;

ALTER TABLE game_variants
    DROP COLUMN name,
    ALTER COLUMN game_id SET NOT NULL,
    ALTER COLUMN environment_id DROP DEFAULT,
    ALTER COLUMN language_id DROP DEFAULT;

CREATE UNIQUE INDEX ON game_variants (game_id, environment_id, language_id);
