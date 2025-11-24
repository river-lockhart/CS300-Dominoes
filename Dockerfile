# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# copy Gradle wrapper + config
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# fix Windows line endings on the wrapper (in case repo was cloned on Windows)
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# download dependencies / Gradle wrapper stuff
RUN ./gradlew --no-daemon help

# copy source
COPY src src

# build three fat jars:
#   1) Linux-targeted JavaFX jar   -> CS300-Dominos-Linux.jar
#   2) Windows-targeted JavaFX jar -> CS300-Dominos-Windows.jar
#   3) Mac-targeted JavaFX jar     -> CS300-Dominos-Mac.jar
RUN ./gradlew --no-daemon \
    -PjavafxPlatform=linux  -PplatformSuffix=Linux  shadowJar && \
    ./gradlew --no-daemon \
    -PjavafxPlatform=win    -PplatformSuffix=Windows shadowJar && \
    ./gradlew --no-daemon \
    -PjavafxPlatform=mac    -PplatformSuffix=Mac    shadowJar


# ---------- export stage ----------
FROM alpine:latest AS export

WORKDIR /artifacts

# bring all three jars from the build stage
COPY --from=build /app/build/libs/CS300-Dominos-Linux.jar   ./CS300-Dominos-Linux.jar
COPY --from=build /app/build/libs/CS300-Dominos-Windows.jar ./CS300-Dominos-Windows.jar
COPY --from=build /app/build/libs/CS300-Dominos-Mac.jar     ./CS300-Dominos-Mac.jar

# at runtime, copy all jars into the mounted /export dir on the host
CMD ["sh", "-c", "cp /artifacts/CS300-Dominos-*.jar /export/ && echo 'CS300-Dominos-Linux.jar, CS300-Dominos-Windows.jar, and CS300-Dominos-Mac.jar have been written to /export' && ls -l /export"]
