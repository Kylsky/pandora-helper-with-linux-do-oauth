CREATE TABLE IF NOT EXISTS `share_claude_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `share_id` bigint NOT NULL,
    `oauth_token` varchar(255),
    `expires_at` datetime,
    `account_id` bigint NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;