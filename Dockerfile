# 使用一个体积更小的 Java 环境基础镜像
FROM openjdk:8-jre-slim

# 设置工作目录
WORKDIR /app
RUN mkdir /app/db
# 将你的 Maven 项目的 target 目录中的 jar 文件复制到镜像中
COPY ./target/linuxdo-oauth-1.0-SNAPSHOT.jar /app/my-app.jar
COPY ./target/classes/db/data.db /app/db/

# 一次性完成所有配置和安装，减少层的数量
RUN apt-get update && \
    apt-get install -y sqlite3 && \
    # 清理 apt 缓存，减少镜像大小
    rm -rf /var/lib/apt/lists/*

# 设置运行 jar 文件的命令
CMD ["java", "-Xmx2048m","-Xms2048m", "-jar", "/app/my-app.jar", "--spring.datasource.url=jdbc:sqlite:/app/db/data.db"]
