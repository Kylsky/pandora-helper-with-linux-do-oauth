ALTER TABLE `midjourney_task`
    MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT FIRST;

ALTER TABLE `account`
    MODIFY COLUMN `access_token` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL AFTER `session_token`;