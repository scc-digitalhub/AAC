CREATE TABLE
    IF NOT EXISTS audit_events (
        event_id varchar(128) PRIMARY KEY,
        event_time TIMESTAMP,
        principal varchar(255),
        realm varchar(255) DEFAULT NULL,
        tx varchar(255),
        event_type varchar(255),
        event_class varchar(255),
        event_data BLOB
    ) ENGINE = InnoDB ROW_FORMAT = DYNAMIC;

CREATE INDEX audit_ix1 ON audit_events (principal);

CREATE INDEX audit_ix2 ON audit_events (realm);

CREATE INDEX audit_ix3 ON audit_events (tx);

CREATE INDEX audit_ix4 ON audit_events (event_class);