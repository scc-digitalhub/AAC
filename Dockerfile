ARG CACHE=ghcr.io/scc-digitalhub/aac:cache
FROM ${CACHE} AS cache

FROM maven:3-openjdk-17 AS build
ARG VER=SNAPSHOT
COPY ./src /tmp/src
COPY ./pom.xml /tmp/pom.xml
COPY ./user-console /tmp/user-console
COPY ./dev-console /tmp/dev-console
COPY ./admin-console /tmp/admin-console
WORKDIR /tmp
RUN --mount=type=cache,target=/root/.m2,source=/cache/.m2,from=cache \ 
    --mount=type=cache,target=/tmp/target/node,source=/cache/target/node,from=cache \ 
    --mount=type=cache,target=/tmp/user-console/node_modules,source=/cache/user-console/node_modules,from=cache \ 
    --mount=type=cache,target=/tmp/dev-console/node_modules,source=/cache/dev-console/node_modules,from=cache \ 
    --mount=type=cache,target=/tmp/admin-console/node_modules,source=/cache/admin-console/node_modules,from=cache \ 
    mvn -Drevision=${VER} package

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /tmp
COPY --from=build /tmp/target/aac.jar aac.jar
RUN java -Djarmode=layertools -jar aac.jar extract


# FROM eclipse-temurin:17-jdk-alpine
# ARG USER=aac
# ARG USER_ID=805
# ARG USER_GROUP=aac
# ARG USER_GROUP_ID=805
# ARG USER_HOME=/home/${USER}
# ENV FOLDER=/tmp/target
# ENV APP=aac.jar
# # create a user group and a user
# RUN  addgroup -g ${USER_GROUP_ID} ${USER_GROUP}; \
#      adduser -u ${USER_ID} -D -g '' -h ${USER_HOME} -G ${USER_GROUP} ${USER} ;

# WORKDIR ${USER_HOME}
# COPY --chown=aac:aac --from=builder dependencies/ ${USER_HOME}
# COPY --chown=aac:aac --from=builder snapshot-dependencies/ ${USER_HOME}
# COPY --chown=aac:aac --from=builder spring-boot-loader/ ${USER_HOME}
# COPY --chown=aac:aac --from=builder application/ ${USER_HOME}
# USER 805
# ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]

FROM gcr.io/distroless/java17-debian12:nonroot
ENV APP=aac.jar
WORKDIR /aac
LABEL org.opencontainers.image.source=https://github.com/scc-digitalhub/AAC
COPY --from=builder /tmp/dependencies/ ./
COPY --from=builder /tmp/snapshot-dependencies/ ./
COPY --from=builder /tmp/spring-boot-loader/ ./
COPY --from=builder /tmp/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]


