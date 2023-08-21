CREATE TABLE
    IF NOT EXISTS oauth_approvals (
        clientId varchar(255) DEFAULT NULL,
        expiresAt timestamp DEFAULT NULL,
        lastModifiedAt timestamp DEFAULT NULL,
        scope varchar(255) DEFAULT NULL,
        status varchar(255) DEFAULT NULL,
        userId varchar(255) DEFAULT NULL
    );

CREATE TABLE
    IF NOT EXISTS oauth_code (
        code VARCHAR(256),
        client_id VARCHAR(256),
        expiresAt TIMESTAMP,
        authentication bytea
    );

CREATE TABLE
    IF NOT EXISTS oauth_access_token (
        token_id VARCHAR(256),
        token bytea,
        authentication_id VARCHAR(256),
        user_name VARCHAR(256),
        client_id VARCHAR(256),
        authentication bytea,
        refresh_token VARCHAR(256)
    );

CREATE TABLE
    IF NOT EXISTS oauth_refresh_token (
        token_id VARCHAR(64) NOT NULL PRIMARY KEY,
        token bytea NOT NULL,
        authentication bytea NOT NULL
    );