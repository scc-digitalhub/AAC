# syntax=docker/dockerfile:experimental
FROM maven:3-openjdk-17 as deps
COPY pom.xml /tmp/build/pom.xml
WORKDIR /tmp/build
#RUN --mount=type=bind,target=/root/.m2,source=/root/.m2,from=smartcommunitylab/aac:cache-alpine mvn package -DskipTests
RUN mvn dependency:go-offline
RUN mvn frontend:install-node-and-yarn
COPY user-console/package.json /tmp/build/user-console/
COPY user-console/yarn.lock /tmp/build/user-console/
RUN mvn frontend:yarn@user-console-yarn-install


FROM maven:3-openjdk-17 as build
COPY --from=deps /root/.m2/. /root/.m2
COPY --from=deps /tmp/build/target /tmp/build/target
COPY ./ /tmp/build
COPY --from=deps /tmp/build/user-console/node_modules/. /tmp/build/user-console/node_modules
WORKDIR /tmp/build
#RUN --mount=type=bind,target=/root/.m2,source=/root/.m2,from=smartcommunitylab/aac:cache-alpine mvn package -DskipTests
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jdk-alpine as builder
COPY --from=build /tmp/build/target/aac.jar aac.jar
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
