FROM maven:3.3-jdk-8 as mvn
COPY . /tmp
WORKDIR /tmp
RUN mvn install -Plocal,authorization

FROM adoptopenjdk/openjdk8:alpine
ARG VER=0.1
ARG USER=aac
ARG USER_ID=805
ARG USER_GROUP=aac
ARG USER_GROUP_ID=805
ARG USER_HOME=/home/${USER}
ENV FOLDER=/tmp/target
ENV APP=aac-${VER}.jar
# create a user group and a user
RUN  addgroup -g ${USER_GROUP_ID} ${USER_GROUP}; \
     adduser -u ${USER_ID} -D -g '' -h ${USER_HOME} -G ${USER_GROUP} ${USER} ;

RUN apk update && apk add curl openssl && rm -rf /var/cache/apk/*
RUN chown aac:aac /opt/java/openjdk/jre/lib/security/cacerts
WORKDIR ${USER_HOME}
COPY --chown=aac:aac --from=mvn /tmp/target/aac.jar ${USER_HOME}
COPY --chown=aac:aac init.sh ${USER_HOME}
USER aac
ENTRYPOINT "/home/aac/init.sh"
