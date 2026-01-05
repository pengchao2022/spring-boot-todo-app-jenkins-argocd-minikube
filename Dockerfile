# 构建阶段
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 pom.xml 和源代码
COPY pom.xml .
COPY src ./src

# 构建应用
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 从构建阶段复制 jar 文件
COPY --from=builder /app/target/*.jar app.jar

# 创建非root用户
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
