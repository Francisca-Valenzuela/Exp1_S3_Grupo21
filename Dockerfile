# ─────────────────────────────────────────────────────────────────────────────
# Etapa 1: Build con Maven
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar descriptor de dependencias primero (optimiza caché de Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─────────────────────────────────────────────────────────────────────────────
# Etapa 2: Imagen final (solo JRE, más liviana)
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear directorio de montaje para EFS
# En producción se mapea con: docker run -v /mnt/efs:/app/efs
RUN mkdir -p /app/efs

# Copiar el JAR generado desde la etapa de build
COPY --from=build /app/target/ms-administracion-archivos-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
