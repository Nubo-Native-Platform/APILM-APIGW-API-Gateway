FROM eclipse-temurin:21-jre
ADD target/api-gateway-1.0.0.jar /api-gateway-1.0.0.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/api-gateway-1.0.0.jar"]
