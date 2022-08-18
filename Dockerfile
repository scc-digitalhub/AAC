# syntax=docker/dockerfile:experimental
FROM maven:3-openjdk-17 as mvn
COPY src /tmp/src
COPY pom.xml /tmp/pom.xml
WORKDIR /tmp
#RUN --mount=type=bind,target=/root/.m2,source=/root/.m2,from=smartcommunitylab/aac:cache-alpine mvn package -DskipTests
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jdk-alpine as builder
COPY --from=mvn /tmp/target/aac.jar aac.jar
RUN java -Djarmode=layertools -jar aac.jar extract


FROM eclipse-temurin:17-jdk-alpine
ARG USER=aac
ARG USER_ID=805
ARG USER_GROUP=aac
ARG USER_GROUP_ID=805
ARG USER_HOME=/home/${USER}
ENV FOLDER=/tmp/target
ENV APP=aac.jar
# create a user group and a user
RUN  addgroup -g ${USER_GROUP_ID} ${USER_GROUP}; \
     adduser -u ${USER_ID} -D -g '' -h ${USER_HOME} -G ${USER_GROUP} ${USER} ;

WORKDIR ${USER_HOME}
COPY --chown=aac:aac --from=builder dependencies/ ${USER_HOME}
COPY --chown=aac:aac --from=builder snapshot-dependencies/ ${USER_HOME}
COPY --chown=aac:aac --from=builder spring-boot-loader/ ${USER_HOME}
COPY --chown=aac:aac --from=builder application/ ${USER_HOME}
USER aac
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
#ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar ${APP}"]
