# 使用一个有 Java 环境的基础镜像
FROM openjdk:11-jdk

# 设置工作目录
WORKDIR /app

# 将你的 Maven 项目的 target 目录中的 jar 文件复制到镜像中
COPY ./target/linuxdo-oauth-1.0-SNAPSHOT.jar /app/my-app.jar
RUN mkdir -p /app/db
ENV SPRING_DATASOURCE_URL jdbc:sqlite:/app/db/data.db

# 设置运行 jar 文件的命令
CMD ["java", "-Xmx2048m","-Xms2048m", "-jar", "/app/my-app.jar", "--spring.datasource.url=${SPRING_DATASOURCE_URL}"]

RUN apt-get update && apt-get install -y sqlite3
