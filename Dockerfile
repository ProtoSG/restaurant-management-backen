# ── Etapa 1: build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copiar solo los archivos de dependencias primero para aprovechar la caché de
# capas: si pom.xml no cambia, Maven no re-descarga nada.
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copiar el resto del código y construir el JAR (sin tests)
COPY src src
RUN ./mvnw clean package -DskipTests -q

# Extraer capas del fat-JAR para que el runtime tenga capas Docker optimizadas
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/layers

# ── Etapa 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S -g 1001 spring && adduser -S -u 1001 -G spring spring

# Copiar las capas del JAR en orden (de menos a más cambiante)
COPY --from=build /app/target/layers/dependencies           ./
COPY --from=build /app/target/layers/spring-boot-loader    ./
COPY --from=build /app/target/layers/snapshot-dependencies ./
COPY --from=build /app/target/layers/application           ./

USER spring

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.launch.JarLauncher"]
