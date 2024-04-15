# syntax=docker/dockerfile:experimental

FROM maven:3-openjdk-17 as build
COPY ./src /tmp/src
COPY ./pom.xml /tmp/pom.xml
COPY ./user-console /tmp/user-console
WORKDIR /tmp
RUN --mount=type=cache,target=/root/.m2,source=/root/.m2,from=smartcommunitylab/aac:cache \ 
    --mount=type=cache,target=/tmp/user-console/node_modules,source=/root/node_modules,from=smartcommunitylab/aac:cache \
    mvn package

FROM eclipse-temurin:17-jdk-alpine as builder
COPY --from=build /tmp/target/aac.jar aac.jar
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

