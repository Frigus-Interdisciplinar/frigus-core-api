# -----------------------------------------------------------------------------
# Estagio 1: Build da aplicacao (Maven + JDK 17)
# -----------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Cache de dependencias do Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compilacao e empacotamento do JAR (ignora testes durante o build da imagem)
COPY src ./src
RUN mvn clean package -DskipTests -B

# -----------------------------------------------------------------------------
# Estagio 2: Imagem final leve para execucao (JRE 17)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Criacao de usuario nao-root para boas praticas de seguranca
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copia o JAR gerado no estagio de build
COPY --from=builder /build/target/*.jar app.jar

# Porta padrao do Spring Boot
EXPOSE 8080

# Otimizacoes de memoria para execucao em container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]