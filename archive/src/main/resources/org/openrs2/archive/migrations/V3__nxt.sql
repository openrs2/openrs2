-- @formatter:off
CREATE TYPE build AS (
    major INTEGER,
    minor INTEGER
);

ALTER TABLE games
    ADD COLUMN key TEXT NULL,
    ADD COLUMN build_minor INTEGER NULL;

ALTER TABLE games
    RENAME COLUMN build TO build_major;

ALTER TABLE sources
    ADD COLUMN build_minor INTEGER NULL;

ALTER TABLE sources
    RENAME COLUMN build TO build_major;

DROP INDEX sources_master_index_id_game_id_build_idx;

CREATE UNIQUE INDEX ON sources (master_index_id, game_id, build_major)
    WHERE type = 'js5remote' AND build_minor IS NULL;

CREATE UNIQUE INDEX ON sources (master_index_id, game_id, build_major, build_minor)
    WHERE type = 'js5remote' AND build_minor IS NOT NULL;

UPDATE games
SET
    url = 'https://www.runescape.com/k=5/l=0/jav_config.ws?binaryType=2',
    build_major = 919,
    build_minor = 1,
    key = $$-----BEGIN PUBLIC KEY-----
MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAnnP2Sqv7uMM3rjmsLTQ3
z4yt8/4j9MDS/2+/9KEkfnH2K/toJbyBUCMHvfS7SBvPiLXWaArNvIArEz/e5Cr3
dk2mcSzmoVcsE1dJq/2eDIqRzhH9WB6zDz+5DO6ysRYap1VdMa4bKXMkM+e7V0c3
9xiMEpjpeSs0cHGxTGlxLBGTFHYG1IZPLkDRJzhKD58Lu8bn2e3KCTuzzvZFf2AF
FZauENC6OswdfAZutlWdWkVOZsD9IB/ALNaY4W35PABZbsfT/ar85S/foXFwHJ+B
OHuF6BR5dYETUQ5Oasl0GUEaVUM9POv7KRv6cW7HWUQHYfQdApjdH+dORHtk4kMG
QAmk/VpTwWBkZWqDbglZBIkd5G7gs8JpluiUh11eRMC/xj99iZp4nt/FOoSNw2NO
GMTUPkHIySC4FQHNSxzbfCW5rQdSRw5+eyuo8MA6mg0LZH3jQuNnnYBg1hJTsdBp
0IrjOQWsfTiX+xZ6lUfRhFtGISuKchpGDZfmOtrZPJDvUgNy0z8w41V6NyiU/h7X
2TKYFQG1/c4Kr4BxT4tPl85nVbMulonfk/AD5l6BflEuHlChpkAhv14j6xRzGHWx
4pdpbHSzDkg/HBR5ka0D7Ua7W6uL3VFVCPAygPERZK1lpYE+m+k92H+i/K7gIV1M
1E07p8x5X9i0oDbZ0lxv8I8CAwEAAQ==
-----END PUBLIC KEY-----
$$
WHERE name = 'runescape';
-- @formatter:on
