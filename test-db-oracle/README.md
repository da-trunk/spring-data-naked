# Building an XE image

  1. Install [docker for windows](https://docs.docker.com/docker-for-windows/install/)
  1. Clone project [docker-images](https://github.com/oracle/docker-images)
    `git clone https://github.com/oracle/docker-images`  
  1. Follow instructions at [OracleDatabase/SingleInstance](https://github.com/oracle/docker-images/tree/main/OracleDatabase/SingleInstance) to build the docker image.  
      1. Download the base image: `docker pull oraclelinux:8-slim`
      1. Execute their script: 
        `cd docker-images/OracleDatabase/SingleInstance/dockerfiles && ./buildContainerImage.sh  -v 21.3.0 -x -t oracle-xe:21.3.0`
      1. Start the image: 
        `docker run -d --name test_database -p 1521:1521 -e ORACLE_PWD=PASSWORD oracle-xe:21.3.0`
  1. Wait for the DB to come up.  Logs will say *DATABASE IS READY TO USE!*.  
    `docker logs -t -f test_database`
  1. Test that the DB is now up.  If this connects to a `SQL>` prompt, it is ready and you may proceed.  Exit the prompt by typing `quit`.
    `docker exec -it test_database sqlplus sys/password@//localhost:1521/XEPDB1 as sysdba`
  1. Clone this repo and switch to the root directory of it (`cd discern-ontology-database`).  Then, use the following process to create a versioned DB in the new docker container.
      1. Build: 
        `mvn install -DskipTests -D "liquibase.username=test"`
      1. (Re)-create the user : 
        `mvn liquibase:update -P schema-drop-create,docker`
      1. Deploy schema: 
        `mvn liquibase:update -P docker -D "liquibase.changeLogFile=master.xml" -D "liquibase.contexts=xe,dev"`
      1. Tag the schema: 
        `mvn liquibase:tag -P docker -D "liquibase.tag=0" -pl test-database`