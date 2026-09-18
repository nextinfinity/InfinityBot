FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY . .
RUN sh ./gradlew --no-daemon shadowJar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /app/build/libs/InfinityBot-*-all.jar /app/InfinityBot.jar
USER 10001:10001
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/app/InfinityBot.jar"]
