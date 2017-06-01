# AAC

## Quickstart project build
 - git clone https://github.com/smartcommunitylab/API-Manager.git
 - move to API-Manager/wso2-integration
 - run `mvn clean install`
 - git clone https://github.com/smartcommunitylab/aac.authorization.git
 - move to aac.authorization
 - run `mvn clean install`

**NOTE if project doesn't compile well, refresh the project in IDE**
 
Local config:
 - From IDE run with -Dspring.profiles.active=local
 - Rename application-local.yml.example to application-local.yml and provide properties
 - Config log folder as `-Daac.log.folder=LOG_FOLDER_PATH` (if system property is not setted, application will use default value: `WORKING_DIRECTORY/logs`)

Execution:
- run the project with Maven: ``mvn -Drun.profiles=local -Djavax.net.ssl.trustStore="C:/bin/wso2am-2.1.0/repository/resources/security/wso2carbon.jks" -Djavax.net.ssl.trustStorePassword="wso2carbon" -Djavax.net.ssl.trustStoreType="JKS" spring-boot:run``  
 
