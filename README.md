# 1.界面展示
<img width="1547" alt="image" src="https://github.com/user-attachments/assets/ffc4484d-3c48-4de3-8ad9-f2e56e965e24">
<img width="1547" alt="image" src="https://github.com/user-attachments/assets/41520e74-3bff-4f6d-bcb3-81646f6232ff">
<img width="1547" alt="image" src="https://github.com/user-attachments/assets/3e145d37-6ac1-4558-a455-04ce7087c1dc">
<img width="1547" alt="image" src="https://github.com/user-attachments/assets/3bee941c-987f-4301-a937-47904c12f57f">
<img width="1547" alt="image" src="https://github.com/user-attachments/assets/53a561cf-105c-498f-b0b2-12d62c6424aa">
<img width="1547" alt="image" src="https://github.com/user-attachments/assets/838b407d-df83-4f42-a5f1-dc811913af7f">

# 2.安装教程
方法1.修改docker-compose.yml里的必填参数，并执行命令：
```
docker-compose up -d 
```

方法2.使用docker直接运行

```
# 首先创建具名卷，将/home/db替换成你的数据库文件目录，目录必须写绝对路径
docker volume create --driver local --opt type=none --opt o=bind --opt device=/home/db db-data
# 运行docker指令
docker run 
-d 
-it
-v db-data:/app/db                                     # 请替换挂载路径
-e TZ=Asia/Shanghai 
-e CLIENT_ID=                                       # 1.你的linux-do oauth client                       
-e CLIENT_SECRET=                                   # 2.你的linux-do oauth secret
-e OAIFREE_PROXY=https://new.oaifree.com            # 3.默认oaifree，你可以填写你的代理地址
-e FUCLAUDE_PROXY=https://demo.fuclaude.com         # 4.默认fuclaude，你可以填写你的fuclaude地址
-e REDIRECT_URI=                                    # 5.你的应用跳转地址
-e ADMIN_NAME=                                      # 6.管理员用户名，建议填写你在linux-do的用户名，默认密码是123456
--restart=always                                    # 7.如需修改端口，请修改第一个8181为你需要访问的服务器端口
-p 8181:8181 --name pandora-helper
 kylsky/pandora_helper_v2
```
下面是一个例子：
```
docker volume create --driver local --opt type=none --opt o=bind --opt device=/home/db db-data

docker run \
-d \
-it \
-v db-data:/app/db \
-e TZ=Asia/Shanghai\ 
-e CLIENT_ID=123 \
-e CLIENT_SECRET=123 \
-e OAIFREE_PROXY=https://new.oaifree.com \
-e FUCLAUDE_PROXY=https://demo.fuclaude.com \
-e REDIRECT_URI=https://my.helper.com \
-e ADMIN_NAME=Admin \
--restart=always \
-p 8181:8181 --name pandora-helper \
 kylsky/pandora_helper_v2
```

# 3.特性&使用教程
特性：
- 支持账号密码、oauth2、激活码三种形式登录使用
- 支持后台切换用户账号，实现用户无感知使用
- 基于oaifree的对话隔离
- 基于fuclaude的对话隔离
- 统一的用户体系，一个账号同时登陆chatgpt、claude及后台
- 支持兑换码形式的账号分发，可二开对接发卡站
- 支持免费号池的使用和搭建
- 支持自动上车、审核上车，支持车上人数限制
- 基于oairfree的用户级别账号用量统计
- access token的定时刷新，减少人工运维成本（需要refresh_token）

使用教程请参考： https://linux.do/t/topic/173810

# 4.如果需要，请联系我
https://linux.do/u/yelo/summary

# 5.特别鸣谢
[始皇大大](https://linux.do/u/neo/summary)

[年华大佬](https://linux.do/u/linux/summary)

[PandoraHelper](https://github.com/nianhua99/PandoraHelper)


## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=Kylsky/pandora-helper-with-linux-do-oauth&type=Date)](https://star-history.com/#Kylsky/pandora-helper-with-linux-do-oauth&Date)
