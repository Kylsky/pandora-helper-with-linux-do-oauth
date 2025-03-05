#docker buildx build --platform linux/amd64 -t kylsky/pandora_helper_v2:latest --load .
docker buildx build --platform linux/arm64 -t kylsky/pandora_helper_v2_arm:latest --load .
#docker push kylsky/pandora_helper_v2:latest
docker push kylsky/pandora_helper_v2_arm:latest
