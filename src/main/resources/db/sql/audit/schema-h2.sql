CREATE TABLE
    IF NOT EXISTS audit_events (
        event_time TIMESTAMP,
        principal varchar(255),
        realm varchar(255) DEFAULT NULL,
        event_type varchar(255),
        event_data BLOB
    );
