# Eclipse Temurin 17 기반 이미지 사용
FROM eclipse-temurin:17-jdk-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 필수 패키지 설치
RUN apk add --no-cache curl tar

# Scouter Agent 다운로드 및 설정
RUN curl -L -o scouter.tar.gz https://github.com/scouter-project/scouter/releases/download/v2.20.0/scouter-all-2.20.0.tar.gz && \
    tar -xzf scouter.tar.gz && \
    mv scouter/agent.java scouter-agent && \
    rm -rf scouter scouter.tar.gz && \
    echo "net_collector_ip=host.docker.internal" > scouter-agent/conf/scouter.conf && \
    echo "net_collector_udp_port=6100" >> scouter-agent/conf/scouter.conf && \
    echo "obj_name=guesthouse-backend" >> scouter-agent/conf/scouter.conf

# JAR 파일 복사
COPY *.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행 (Scouter Agent 적용)
ENTRYPOINT ["java", "--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED", "-javaagent:/app/scouter-agent/scouter.agent.jar", "-Dscouter.config=/app/scouter-agent/conf/scouter.conf", "-jar", "app.jar"]
