docker buildx build --platform linux/amd64 -t linuxdo-oauth2-java:latest --load .
docker save linuxdo-oauth2-java:latest -o linux-do-oauth2.tar