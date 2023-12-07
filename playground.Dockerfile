FROM node:18 AS build
RUN apt-get update && apt-get install openjdk-17-jdk -y
WORKDIR /workspace/
COPY . /workspace/
RUN --mount=type=cache,target=/root/.gradle ./gradlew clean build -x test
RUN mkdir -p server/build/dependency && (cd server/build/dependency; jar -xf ../libs/jifa.jar)

FROM eclipse-temurin:17-jdk
VOLUME /tmp
ARG DEPENDENCY=/workspace/server/build/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /jifa/lib
COPY --from=build ${DEPENDENCY}/META-INF /jifa/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /jifa
ADD https://raw.githubusercontent.com/wiki/eclipse/jifa/resources/playground/thread-dump-demo.log /demo/
ADD https://raw.githubusercontent.com/wiki/eclipse/jifa/resources/playground/gc-log-demo.log /demo/
ADD https://raw.githubusercontent.com/wiki/eclipse/jifa/resources/playground/heap-dump-demo.hprof.gz /demo/
ENTRYPOINT ["java","--add-opens=java.base/java.lang=ALL-UNNAMED","--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED","-Djdk.util.zip.disableZip64ExtraFieldValidation=true","-cp","jifa:jifa/lib/*","org.eclipse.jifa.server.Launcher","--jifa.role=standalone-worker","--jifa.storage-path=/jifa-storage","--jifa.input-files[0]=/demo/thread-dump-demo.log","--jifa.input-files[1]=/demo/gc-log-demo.log","--jifa.input-files[2]=/demo/heap-dump-demo.hprof.gz"]