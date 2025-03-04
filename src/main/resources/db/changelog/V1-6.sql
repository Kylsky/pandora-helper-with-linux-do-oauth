alter table share
    add mj_proxy_url TEXT;

alter table share
    add mj_proxy_key TEXT;

alter table share
    add chat_gpt_url TEXT;

alter table share
    add chat_gpt_password TEXT;

CREATE TABLE midjourney_task (
                                 "id" INTEGER NOT NULL,
                                 "user_name" varchar(255),
                                 "task_id" varchar(255) NOT NULL,
                                 "create_time" datetime,
                                 PRIMARY KEY ("id")
);