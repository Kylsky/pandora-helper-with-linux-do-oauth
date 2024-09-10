docker buildx build --platform linux/amd64 -t linuxdo-oauth2-java:latest --load .
docker save linuxdo-oauth2-java:latest -o pandora-helper.tar
scp -P 10022 pandora-helper.tar yeelo@mysh.yeelo.fun:/home/oauth
#scp -P 22 pandora-helper.tar root@192.227.138.159:/root/helper-template