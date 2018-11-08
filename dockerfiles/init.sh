#!/bin/sh
cd ${AAC_HOME}/

rm -f cert.pem && echo -n | openssl s_client -connect api-manager:9443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ./cert.pem

keytool -import -trustcacerts -file cert.pem -alias root -keystore $JAVA_HOME/jre/lib/security/cacerts -storepass changeit -noprompt

${MVN_BIN}/mvn -Plocal spring-boot:run
