CREATE TABLE
    IF NOT EXISTS provider_config (
        type VARCHAR(256),
        provider_id VARCHAR(256),
        realm VARCHAR(256),
        config BLOB,
        PRIMARY KEY (provider_id, type)
    )
CREATE TABLE
    IF NOT EXISTS attributes (
        entity_id VARCHAR(256),
        provider_id VARCHAR(256),
        attr_key VARCHAR(256),
        attr_value BLOB
    )