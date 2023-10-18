FROM eclipse-temurin:17-jdk
VOLUME /tmp
ARG TARGETARCH
COPY jifa-build/$TARGETARCH/BOOT-INF/lib /jifa/lib
COPY jifa-build/$TARGETARCH/META-INF /jifa/META-INF
COPY jifa-build/$TARGETARCH/BOOT-INF/classes /jifa
ENTRYPOINT ["java","--add-opens=java.base/java.lang=ALL-UNNAMED","--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED","-Djdk.util.zip.disableZip64ExtraFieldValidation=true","-cp","jifa:jifa/lib/*","org.eclipse.jifa.server.Launcher"]