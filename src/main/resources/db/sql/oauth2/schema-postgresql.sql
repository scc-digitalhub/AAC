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
        authentication_id VARCHAR(256),
        user_name VARCHAR(256),
        client_id VARCHAR(256),        
        authentication bytea NOT NULL
    );

CREATE INDEX oauth_access_token_token_id_index ON public.oauth_access_token (token_id);
CREATE INDEX oauth_access_token_token_user_index ON public.oauth_access_token (user_name);
CREATE INDEX oauth_access_token_token_client_index ON public.oauth_access_token (client_id);
CREATE INDEX oauth_access_token_token_refresh_token_index ON public.oauth_access_token (refresh_token);

CREATE INDEX oauth_refresh_token_token_id_index ON public.oauth_refresh_token (token_id);
CREATE INDEX oauth_refresh_token_token_user_index ON public.oauth_refresh_token (user_name);
CREATE INDEX oauth_refresh_token_token_client_index ON public.oauth_refresh_token (client_id);