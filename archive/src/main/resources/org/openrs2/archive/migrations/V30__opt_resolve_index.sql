-- @formatter:off

CREATE OR REPLACE FUNCTION resolve_index(_scope_id INTEGER, _archive_id uint1, _crc32 INTEGER, _version INTEGER) RETURNS SETOF containers AS $$
    SELECT c.*
    FROM groups g
    JOIN containers c ON c.id = g.container_id
    JOIN indexes i ON i.container_id = c.id
    WHERE g.scope_id = _scope_id AND g.archive_id = 255::uint1 AND g.group_id = _archive_id::INTEGER AND c.crc32 = _crc32 AND
        g.version = _version AND NOT g.version_truncated AND i.version = _version
    ORDER BY c.id ASC
    LIMIT 1;
$$ LANGUAGE SQL STABLE PARALLEL SAFE ROWS 1;
