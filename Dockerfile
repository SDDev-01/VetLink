# ==========================================
# ETAPA 1: BUILD
# ==========================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copiamos Maven Wrapper y configuración
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Damos permisos de ejecución al wrapper
RUN chmod +x mvnw

# Copiamos el código fuente
COPY src/ src/

# Compilamos la aplicación
RUN ./mvnw -B clean package -DskipTests


# ==========================================
# ETAPA 2: RUNTIME
# ==========================================
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

# Copiamos el JAR generado en la etapa anterior
COPY --from=build /app/target/vetlink-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]