# Imagen para desplegar el backend en Render (Web Service con Docker)

# 1) Compilación con Gradle y JDK 21
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

# 2) Ejecución solo con el JRE (imagen más liviana)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# Render define la variable PORT; application.yml la usa
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
