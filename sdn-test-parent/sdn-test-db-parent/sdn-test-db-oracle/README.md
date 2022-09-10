# Create Database

```sh
sqlplus sys/password@localhost as sysdba
SQL> ALTER SESSION SET CONTAINER=XEPDB1;
SQL> CREATE USER TEST IDENTIFIED BY password QUOTA UNLIMITED ON USE
SQL> GRANT CONNECT, RESOURCE, CREATE SESSION, create table TO TEST;
SQL> quit
sqlplus test/test@//localhost/XEPDB1
```

# Compare entities to dev and generate changeset to migrate dev to entities

`mvn liquibase:diff`
  
  * This may include drop statements for DB objects not represented in the entities.  See next sections to remove those.
  * Before generating your migration, be sure to delete the migration output file if it already exists.  If you don't, the new commands will be appended to it.

# Exclude objects from the diff

  Note: there is already a pattern matcher in `pom.xml`.  This matches tables which start with a lower case letter.  It also matches dependencies of those tables.  This was done to include our JPA entities without including anything that we haven't modeled as an entity.

  * If the `<diffIncludeObject>` in the `pom.xml` doesn't work, you may need to comment it out and do things manually.  See examples below:
    * Only include consumer and task tables and their dependent objects: `mvn liquibase:diff -D 'liquibase.diffIncludeObjects="table:consumer.*,table:.*task.*"'`
    * Exclude objects with name starting with consumer regardless of case.  Also exclude dependencies of these tables: `mvn liquibase:diff -D 'liquibase.diffExcludeObjects="(i?)consumer.*"'`
    * See [documentation](https://docs.liquibase.com/workflows/liquibase-community/including-and-excluding-objects-from-a-database.html) for more examples.
  
# Remove all drop statements from an XML changeset

  If inclusions / exclusion don't work.

  * Open *Notepad++*
  * Search and replace:
    * Search Mode = *Regular expression*
    * search: `<changeSet[^>]*>(?s)\s+<drop\w+ ([^>]+?)>\s+<\/changeSet>\s+(?-s)`
    * replace:
    
# Starting the image manually

* `docker pull gvenzl/oracle-xe:21.3.0-slim-faststart`
* `docker run -d --name db -p 1521:1521 --shm-size=1g -e ORACLE_PASSWORD=password -e ORACLE_PDB=XEPDB1 gvenzl/oracle-xe:21.3.0-slim-faststart`
    
# Building an XE image

  1. Install [docker for windows](https://docs.docker.com/docker-for-windows/install/)
  1. Clone project [docker-images](https://github.com/oracle/docker-images)
    `git clone https://github.com/oracle/docker-images`  
  1. Follow instructions at [OracleDatabase/SingleInstance](https://github.com/oracle/docker-images/tree/main/OracleDatabase/SingleInstance) to build the docker image.  
      1. Download the base image: `docker pull oraclelinux:8-slim`
      1. Execute their script: 
        `cd docker-images/OracleDatabase/SingleInstance/dockerfiles && ./buildContainerImage.sh  -v 21.3.0 -x -t oracle-xe:21.3.0 -o '--build-arg SLIMMING=false'`
      1. Start the image: 
        `docker run -d --name db -p 1521:1521 --shm-size=1g -e ORACLE_PWD=PASSWORD -e ORACLE_PDB=XEPDB1 oracle-xe:21.3.0`
  1. Wait for the DB to come up.  Logs will say *DATABASE IS READY TO USE!*.  
    `docker logs -t -f db`
  1. Test that the DB is now up.  If this connects to a `SQL>` prompt, it is ready and you may proceed.  Exit the prompt by typing `quit`.
    `docker exec -it db sqlplus sys/password@//localhost:1521/XEPDB1 as sysdba`
  1. Create the test user.
  1. Execute the liquibase changesets you built to deploy schema in the new docker container.
      1. Build: 
        `mvn liquibase:update -D "liquibase.changeLogFile=<diffOutput>"`
      1. Tag the schema: 
        `mvn liquibase:tag -D "liquibase.tag=0"`
       