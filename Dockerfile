FROM gradle:jdk10 as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar

FROM anapsix/alpine-java:10
COPY --from=builder /home/gradle/src/infinitybot/build/distributions/infinitybot.jar ./InfinityBot.jar
MAINTAINER NextInfinity

ENTRYPOINT java -jar "InfinityBot.jar"