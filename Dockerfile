# syntax=docker/dockerfile:1
# =============================================================================
# 后端生产镜像（S25）
#
# 构建：docker build -t sd-backend:local .
#       （必须在 RuoYi-Vue-Plus 仓库根执行，上下文就是仓库根；父目录 sd-blog/ 里有游离的
#         target/，绝不能当上下文——AGENTS.md §8）
#
# 运行：所有环境相关地址都由环境变量注入，镜像内不含任何生产密钥。
#   SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL / _USERNAME / _PASSWORD
#   SPRING_DATA_REDIS_HOST / _PORT / _PASSWORD
#   SPRING_ELASTICSEARCH_URIS
#   JAVA_OPTS（堆大小等，默认 -Xms512m -Xmx1024m）
#
# 为什么用 BuildKit cache mount 而不是逐层 COPY pom：
#   本项目 5 个一级模块 + 7 个子模块，逐层 COPY 需要精确列出全部 pom 路径，漏一个就静默
#   退化成全量下载；cache mount 把 /root/.m2 跨构建持久化，改代码时依赖不重下，且不写进镜像层。
# =============================================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 源码整体拷入（target/.git/logs 等由 .dockerignore 排除）
COPY . .

# -Pprod：把 @profiles.active@ 资源过滤成 prod（否则镜像里带的是 dev 配置）
# -DskipTests：镜像构建不跑单测（单测在 CI 里跑）
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -Pprod -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre-alpine

# tzdata：容器内 Asia/Shanghai 时区需要它，否则日志与数据库时间会偏 8 小时
RUN apk add --no-cache tzdata curl \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app
COPY --from=build /build/ruoyi-admin/target/ruoyi-admin.jar ./app.jar

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1024m"

EXPOSE 8081

# exec 形式让 java 成为 PID 1，SIGTERM 能正常转发（容器停止时优雅退出）
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -jar app.jar"]
