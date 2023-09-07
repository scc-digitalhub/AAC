CREATE TABLE
    IF NOT EXISTS audit (
        time TIMESTAMP,
        principal varchar(255),
        realm varchar(255) DEFAULT NULL,
        type varchar(255),
        event BLOB
    )