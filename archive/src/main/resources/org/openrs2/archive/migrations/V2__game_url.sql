-- @formatter:off
ALTER TABLE games
    DROP COLUMN hostname,
    DROP COLUMN port,
    ADD COLUMN url TEXT NULL;

UPDATE games
SET url = 'https://oldschool.runescape.com/jav_config.ws'
WHERE name = 'oldschool';
