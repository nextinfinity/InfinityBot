FROM gradle:jdk11 as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar

FROM openjdk:11
COPY --from=builder /home/gradle/src/build/libs/InfinityBot-*-all.jar ./InfinityBot.jar
MAINTAINER NextInfinity

ENTRYPOINT java -jar "InfinityBot.jar"