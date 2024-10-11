-- liquibase formatted changelog

/*
 Navicat Premium Dump SQL

 Source Server         : 模版
 Source Server Type    : SQLite
 Source Server Version : 3045000 (3.45.0)
 Source Schema         : main

 Target Server Type    : SQLite
 Target Server Version : 3045000 (3.45.0)
 File Encoding         : 65001

 Date: 10/10/2024 17:18:32
*/

PRAGMA foreign_keys = false;

-- ----------------------------
-- Table structure for account
-- ----------------------------
CREATE TABLE IF NOT EXISTS "account"
(
    id            integer
        primary key,
    email         text,
    password      text,
    session_token text,
    access_token  text,
    create_time   datetime,
    update_time   datetime,
    shared        integer,
    refresh_token text,
    user_id       integer           not null,
    account_type  integer default 1 not null,
    auto          integer default 0 not null,
    user_limit    int     default 4 not null
    , name varchar(255));

-- ----------------------------
-- Table structure for car_apply
-- ----------------------------
CREATE TABLE IF NOT EXISTS "car_apply" (
                             "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                             "share_id" INTEGER NOT NULL,
                             "account_id" INTEGER NOT NULL
);

-- ----------------------------
-- Table structure for redemption
-- ----------------------------
CREATE TABLE IF NOT EXISTS "redemption"
(
    id               INTEGER           not null
        constraint redemption_pk
            primary key autoincrement,
    target_user_name varchar(255),
    account_id       integer           not null,
    user_id          integer           not null,
    duration         integer           not null,
    time_unit        integer default 1 not null,
    create_time      datetime          not null,
    code             varchar(255)      not null
);

-- ----------------------------
-- Table structure for share
-- ----------------------------
CREATE TABLE IF NOT EXISTS "share"
(
    id          integer
        primary key autoincrement,
    account_id  integer
        constraint fk_account_shares
            references account
            on delete cascade,
    unique_name text,
    password    text,
    comment     text,
    expires_at  text,
    parent_id   integer default 1 not null,
    avatar_url  varchar(255),
    trust_level integer
);

-- ----------------------------
-- Table structure for share_claude_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS "share_claude_config" (
                                       "id" INTEGER NOT NULL,
                                       "share_id" INTEGER NOT NULL,
                                       "oauth_token" varchar(255),
                                       "expires_at" datetime,
                                       "account_id" INTEGER NOT NULL,
                                       PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for share_gpt_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS "share_gpt_config" (
                                    "id" INTEGER NOT NULL,
                                    "share_id" INTEGER NOT NULL,
                                    "share_token" text,
                                    "expires_in" integer,
                                    "expires_at" text,
                                    "site_limit" text,
                                    "gpt4_limit" integer,
                                    "gpt35_limit" integer,
                                    "show_userinfo" numeric,
                                    "show_conversations" numeric,
                                    "refresh_everyday" numeric,
                                    "temporary_chat" numeric,
                                    "account_id" INTEGER NOT NULL,
                                    PRIMARY KEY ("id")
);

CREATE TABLE IF NOT EXISTS "share_api_config" (
                                    "id" INTEGER NOT NULL,
                                    "share_id" INTEGER NOT NULL,
                                    "account_id" INTEGER NOT NULL,
                                    "api_proxy" varchar(255),
                                    "api_key" varchar(255),
                                    PRIMARY KEY ("id")
);

PRAGMA foreign_keys = true;
