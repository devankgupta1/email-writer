# force change
FROM maven:3.9.6-eclipse-temurin-17-alpine

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["sh", "-c", "java -jar target/*.jar"]