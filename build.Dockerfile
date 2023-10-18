FROM node:18
RUN apt-get update && apt-get install openjdk-17-jdk -y
WORKDIR /workspace/
COPY . /workspace/
ARG GRADLE_ARGS
RUN --mount=type=cache,target=/root/.gradle eval set -- $GRADLE_ARGS &&  ./gradlew $@
RUN mkdir -p server/build/dependency && (cd server/build/dependency; jar -xf ../libs/jifa.jar)