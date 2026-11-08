# Imagem da API (multi-stage). Contexto de build: apps/api.
# APP_VERSION (SHA do commit no deploy) aparece em /actuator/info.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package && cp target/agro-api-*.jar app.jar

FROM eclipse-temurin:17-jre
ARG APP_VERSION=dev
ENV APP_VERSION=${APP_VERSION}
RUN useradd --system --uid 10001 --no-create-home agro
USER agro
WORKDIR /app
COPY --from=build /build/app.jar app.jar
EXPOSE 8080
# Heap baixo: a VPS de produção é compartilhada (ADR-0020).
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "app.jar"]
