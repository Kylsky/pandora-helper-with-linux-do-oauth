CREATE TABLE "share_grok_config"
(
    "id"          INTEGER NOT NULL,
    "share_id"    INTEGER NOT NULL,
    "oauth_token" varchar(255),
    "expires_at"  datetime,
    "account_id"  INTEGER NOT NULL,
    PRIMARY KEY ("id")
);