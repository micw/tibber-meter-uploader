FROM maven:3-openjdk-17 as builder

ADD . /src

WORKDIR /src

RUN mvn -B package && chmod 0755 /src/target/tibber-meter-uploader-1.0.0-SNAPSHOT.jar

FROM openjdk:17-alpine

COPY --from=builder /src/target/tibber-meter-uploader-1.0.0-SNAPSHOT.jar /tibber-meter-uploader-1.0.0-SNAPSHOT.jar

RUN apk add --no-cache curl jq bash

CMD /tibber-meter-uploader-1.0.0-SNAPSHOT.jar
