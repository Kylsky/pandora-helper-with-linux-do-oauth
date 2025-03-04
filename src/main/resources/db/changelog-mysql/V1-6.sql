alter table share
    add COLUMN mj_proxy_url TEXT null;

alter table share
    add COLUMN mj_proxy_key TEXT null;

alter table share
    add  COLUMN chat_gpt_url TEXT null;

alter table share
    add COLUMN chat_gpt_password TEXT null;

CREATE TABLE midjourney_task (
                                 id INTEGER NOT NULL,
                                 user_name varchar(255),
                                 task_id varchar(255) NOT NULL,
                                 create_time datetime,
                                 PRIMARY KEY (id)
);

