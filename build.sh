docker buildx build --platform linux/amd64 -t pandora_helper_v2:latest --load .
docker tag pandora_helper_v2:latest kylsky/pandora_helper_v2:latest
docker push kylsky/pandora_helper_v2:latest