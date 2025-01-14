FROM openjdk:21-slim

# copy app
COPY build/libs/*-all.jar app.jar

# install proc utils
RUN apt-get update && apt-get install -y procps && rm -rf /var/lib/apt/lists/*

ENTRYPOINT ["java","-jar","/app.jar"]