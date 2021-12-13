# Start with a base image containing Java runtime
FROM adoptopenjdk/openjdk8:alpine-slim

# Log4j mitigation https://www.docker.com/blog/apache-log4j-2-cve-2021-44228/
ENV LOG4J_FORMAT_MSG_NO_LOOKUPS=true

# Add Maintainer Info
LABEL maintainer="sahajamit@gmail.com"

VOLUME /tmp

# The application's jar file
ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} app/grid-utils.jar

WORKDIR app

# Run the jar file
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","grid-utils.jar"]