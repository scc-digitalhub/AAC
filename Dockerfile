FROM ubuntu:16.04

# set user configuration
ARG USER=aac
ARG USER_ID=801
ARG USER_GROUP=aac
ARG USER_GROUP_ID=801
ARG USER_HOME=/home/${USER}
# set files directory
ARG FILES=./dockerfiles
# set jdk conf
ARG JDK=jdk1.8.0*
ARG JAVA_HOME=${USER_HOME}/java
ARG AAC=aac
ARG AAC_HOME=${USER_HOME}/aac/
ARG MVN=apache-maven*
ARG MVN_BIN=${USER_HOME}/mvn/bin

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    curl \
    netcat \
    openssl

# create a user group and a user
RUN groupadd --system -g ${USER_GROUP_ID} ${USER_GROUP} && \
    useradd --system --create-home --home-dir ${USER_HOME} --no-log-init -g ${USER_GROUP_ID} -u ${USER_ID} ${USER}

# copy the jdk and aac 
COPY --chown=aac:aac ${FILES}/${JDK} ${USER_HOME}/java/
COPY --chown=aac:aac ./src/ ${USER_HOME}/aac/
COPY --chown=aac:aac ./pom.xml ${USER_HOME}/aac/
COPY --chown=aac:aac ${FILES}/${MVN} ${USER_HOME}/mvn/
COPY --chown=aac:aac ${FILES}/init.sh ${USER_HOME}/

# set the user and work directory
USER ${USER_ID}
WORKDIR ${USER_HOME}

# set environment variables
ENV JAVA_HOME=${JAVA_HOME} \
    PATH=$JAVA_HOME/bin:$PATH \
    MVN_BIN=${MVN_BIN} \
    AAC_HOME=${AAC_HOME} \
    WORKING_DIRECTORY=${USER_HOME}

# expose ports
EXPOSE 8080 9443

ENTRYPOINT ${WORKING_DIRECTORY}/init.sh
