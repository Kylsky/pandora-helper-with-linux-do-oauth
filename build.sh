docker buildx build --platform linux/amd64 -t linuxdo-oauth2-java:latest --load .
docker save linuxdo-oauth2-java:latest -o pandora-helper-vue-version.tar
scp -P 10022 pandora-helper-vue-version.tar yeelo@mysh.yeelo.fun:/home/oauth-test
#scp -P 22 pandora-helper.tar root@192.227.138.159:/root/helper-template