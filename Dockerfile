# -----------------------------------------------------------------------------
# Estagio 1: Build da aplicacao (Maven + JDK 17)
# -----------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Cache de dependencias do Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080
# Portas do container (8080 padrao, 10000 do Render)
EXPOSE 8080 10000

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
# Otimizacoes de memoria para execucao em container (Render Free 512MB) e IPv4
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=60.0 -XX:+UseSerialGC -Xss256k -Djava.net.preferIPv4Stack=true"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]