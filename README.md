# AAC

Local config:
 - From IDE run with -Dspring.profiles.active=local
 - Rename application-local.yml.example to application-local.yml and provide properties
 - Config log folder as `-Daac.log.folder=LOG_FOLDER_PATH` (if system property is not setted, application will use default value: `WORKING_DIRECTORY/logs`)

Execution:
 - run the project with Maven: ``mvn -Drun.profiles=local spring-boot:run`` 
 
