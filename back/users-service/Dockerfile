
# Usar imagen base de OpenJDK 17
FROM eclipse-temurin:17-jdk-alpine as builder

# Establecer directorio de trabajo
WORKDIR /app

# Copiar archivos de Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Dar permisos de ejecución al wrapper de Maven
RUN chmod +x ./mvnw

# Descargar dependencias (aprovecha caché de Docker)
RUN ./mvnw dependency:go-offline

# Copiar código fuente
COPY src src

# Construir la aplicación
RUN ./mvnw clean package -DskipTests

# Etapa final - imagen mínima para ejecutar
FROM eclipse-temurin:17-jre-alpine

# Instalar curl para health checks
RUN apk add --no-cache curl

# Crear usuario no-root para seguridad
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser

# Crear directorio de la aplicación
WORKDIR /app

# Copiar JAR desde la etapa de construcción
COPY --from=builder /app/target/*.jar app.jar

# Cambiar ownership del directorio
RUN chown -R appuser:appuser /app

# Cambiar a usuario no-root
USER appuser

# Exponer puerto
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]