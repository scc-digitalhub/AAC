CREATE TABLE
    IF NOT EXISTS provider_config (
        provider_type VARCHAR(256),
        provider_id VARCHAR(256),
        realm VARCHAR(256),
        config BLOB,
        PRIMARY KEY (provider_id, provider_type)
    ) ENGINE = InnoDB ROW_FORMAT = DYNAMIC;

CREATE INDEX provider_config_ix1 ON provider_config (provider_type);

CREATE INDEX provider_config_ix2 ON provider_config (provider_id);

CREATE INDEX provider_config_ix2 ON provider_config (realm);

CREATE TABLE
    IF NOT EXISTS attributes (
        entity_id VARCHAR(256),
        provider_id VARCHAR(256),
        attr_key VARCHAR(256),
        attr_value BLOB
    );
    
CREATE TABLE
    IF NOT EXISTS files (
        id VARCHAR(256),
        file_id VARCHAR(256),
        data LONGBLOB,
        realm VARCHAR(256),
        size BIGINT
    );