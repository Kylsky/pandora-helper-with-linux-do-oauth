# 1.安装教程


方法1.修改docker-compose.yml里的必填参数，并执行命令：
```
docker-compose up -d 
```

方法2.使用docker直接运行
```
docker run 
-d 
-it 
-e TZ=Asia/Shanghai 
-e CLIENT_ID=                                       # 1.你的linux-do oauth client                       
-e CLIENT_SECRET=                                   # 2.你的linux-do oauth secret
-e OAIFREE_PROXY=https://new.oaifree.com            # 3.默认oaifree，你可以填写你的代理地址
-e FUCLAUDE_PROXY=https://demo.fuclaude.com         # 4.默认fuclaude，你可以填写你的fuclaude地址
-e REDIRECT_URI=                                    # 5.你的应用跳转地址
-e ADMIN_NAME=                                      # 6.管理员用户名，建议填写你在linux-do的用户名
--restart=always                                    # 7.如需修改端口，请修改第一个8181为你需要访问的服务器端口
-p 8181:8181 --name pandora-helper
 kylsky/pandora_helper
```
下面是一个例子：
```
docker run 
-d 
-it 
-e TZ=Asia/Shanghai 
-e CLIENT_ID=123                                                              
-e CLIENT_SECRET=123                               
-e OAIFREE_PROXY=https://new.oaifree.com            
-e FUCLAUDE_PROXY=https://demo.fuclaude.com         
-e REDIRECT_URI=https://my.helper.com                                     
-e ADMIN_NAME=Admin                                 
--restart=always                                    
-p 8181:8181 --name pandora-helper
 kylsky/pandora_helper
```

# 2.使用教程
请参考： https://linux.do/t/topic/173810

# 3.如果需要，请联系我
https://linux.do/u/yelo/summary

# 4.特别鸣谢
[始皇大大](https://linux.do/u/neo/summary)

[年华大佬](https://linux.do/u/linux/summary)

[PandoraHelper](https://github.com/nianhua99/PandoraHelper)
