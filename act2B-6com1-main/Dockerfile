# ---------- BUILD ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos solo el pom primero para cachear dependencias
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Ahora copiamos el código
COPY . .

# Compilamos y empaquetamos
RUN mvn -B package

# ---------- RUNTIME ----------
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copiamos el jar generado
COPY --from=build /app/target/*.jar app.jar

# Puerto (si tu app expone alguno)
EXPOSE 8080

# Ejecutamos la app
CMD ["java", "-jar", "app.jar"]
