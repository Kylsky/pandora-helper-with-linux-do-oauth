docker buildx build --platform linux/amd64 -t linuxdo-oauth2-java:latest --load .
docker save linuxdo-oauth2-java:latest -o linux-do-oauth2.tar
scp -P 10022 linux-do-oauth2.tar yeelo@mysh.yeelo.fun:/home/oauth
#scp -P 22 linux-do-oauth2.tar root@192.227.138.159:/root/myhelper