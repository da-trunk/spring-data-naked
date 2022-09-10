# sdn-test-server-tomcat

This project demos spring-data-rest on Tomcat.  During tests, it works with `sdn-test-db-mysql` to start two docker containers (one with a MySQL database and one with a generic tomcat container), deploy `target/sdn-test-server-tomcat-<version>.war` and `src/main/resource/conf/` configuration files to the tomcat container, and execute some tests using the entities at `sdn-test-entities`.

# Quick Start

1. Start a MySQL database: `mvn spring-boot:run -pl ./sdn-test-parent/sdn-test-db-parent/sdn-test-db-mysql`.  The DB is started at `jdbc:mysql://localhost:3306/test` with user/login `test/password`.
1. Start the server: `mvn spring-boot:run -pl ./sdn-test-parent/sdn-test-server-tomcat`
1. Browse to http://localhost:9080/api
1. See [sdn-test-db-parent](../sdn-test-db-parent/README.md)) for other databases.  If you try another one, be sure to edit `src/main/application-default.yml`.

# Miscellaneous

  * Build: `mvn clean package`
  * Executing with docker-compose: `docker-compose up --build -d`
  * Connecting with MySQL Workbench:
    * username: `test`
    * password: `password`
    * hostname: `localhost`
    * port: `3306`
  * Deploying the image:
    * `docker container commit tomcat tomcat/test`
    * `docker image tag tomcat/test <tag>`
    * `docker push <tag>`
  * Troubleshooting the container:
      `docker exec -it tomcat bash` or `docker-compose exec tomcat bash`
      `apt-get update && apt-get install vim -y`
  * Tearing down the container:
    * `docker-compose down`
    * `docker system prune -f`
  * Executing with `Dockerfile`: 
    1. Create a network: `docker network create -d bridge mynetwork`
    1. Start the db container.
    1. Connect the db container to your network: `docker network connect mynetwork <container> --alias db`
    1. `docker build -t test-api .`
    1. `docker run -d --name tomcat --network mynetwork -p 9080:8080 test-api:latest`
  * Executing without a container:
    1. `mvn spring-boot:run`
  * Troubleshooting:
    * `docker run -it --entrypoint=bash <image>` or `docker exec -it <container> bash`
    * Copy file from local to (even stopped) container: `docker cp start.sh <container>:/usr/local/tomcat/bin/start.sh`
  * Deploy from container:
      1. Create image from container: `docker container commit <container> <image>`
      1. Tag: `docker image tag <image> <tag>`
      1. Push: `docker push <tag>`
  * Test: NOTE: Avoid making changes directly to the container.  Instead, modify `docker-compose.yml` and `Dockerfile`.  
      1. `docker pull <image>`
      1. `docker container rm tomcat`
      1. `docker run -it --name tomcat --entrypoint=bash --network mynetwork <tag>`
    
# URLs

  * [manager](http://localhost:9080/manager) `tomcat/password`
  * [endpoint listing](http://localhost:9080/api)
  * [users](http://localhost:9080/api/users)
      
# Example Requests
  * `curl -s http://localhost:9080/api/users/Bob`
  * `curl -i -X POST -H "Content-Type:application/json" -d '{ \"name\": \"Bob\" }' http://localhost:9080/api/users`
  * `curl -i -X PATCH -H "Content-Type: application/json-patch+json" -d '[ {"op":"add","path":"/users/-","value":"Bob"} ]' http://localhost:9080/api/users/1`
  * `curl -i -X DELETE http://localhost:9080/api/users/1`
  
# Running tests manually

  * Start a database.  See [sdn-test-db-parent](../sdn-test-db-parent/README.md)) for code to start an empty database.
  * Edit `application-default.yml` to point to the existing database and service.
  * Start the server: `mvn spring-boot:run` (from the root directory of this repository)
  * Run tests
  
# Migrating the database

  * Start a database and server container.
  * Generate change sets from the entities created by test-server: `mvn liquibase:diff`
  * Deploy change sets to another database: `mvn liquibase:update`
      