FROM --platform=linux/amd64 eclipse-temurin:17.0.5_8-jdk-alpine
WORKDIR /wd

ARG BUILD_JAR=jifa.jar

COPY ./server/build/libs/${BUILD_JAR} jifa.jar

CMD ["java","-jar","/wd/jifa.jar"]
