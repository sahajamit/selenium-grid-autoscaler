# Start with a base image containing Java runtime
FROM adoptopenjdk/openjdk8:alpine-slim

# Add Maintainer Info
LABEL maintainer="sahajamit@gmail.com"

VOLUME /tmp

# The application's jar file
ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} app/grid-utils.jar

WORKDIR app

# Run the jar file
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","grid-utils.jar"]