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
    );