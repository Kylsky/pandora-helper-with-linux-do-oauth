docker buildx build --platform linux/amd64 -t linuxdo-oauth2-java:latest --load .
docker save linuxdo-oauth2-java:latest -o linux-do-oauth2.tar
scp -P 10022 linux-do-oauth2.tar yeelo@yeelo.asuscomm.cn:/home/oauth
#ssh -p 10022 yeelo@yeelo.asuscomm.cn "sh /home/oauth/start.sh"