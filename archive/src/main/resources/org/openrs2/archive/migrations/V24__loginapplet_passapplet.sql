-- @formatter:off

INSERT INTO scopes (name) VALUES ('loginapplet');
INSERT INTO games (name, scope_id) VALUES ('loginapplet', (SELECT id FROM scopes WHERE name = 'loginapplet'));

INSERT INTO scopes (name) VALUES ('passapplet');
INSERT INTO games (name, scope_id) VALUES ('passapplet', (SELECT id FROM scopes WHERE name = 'passapplet'));
