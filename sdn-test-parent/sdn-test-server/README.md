# Executing locally

  * Build: `mvn clean package`
  * Execute with docker-compose: 
    * `docker-compose up --build -d`
    * Point SQL Developer to:
      * username: `cm_test`
      * password: `password`
      * hostname: `localhost`
      * port: `1521`
      * service name: `XE`
    * deploy:
      * `docker container commit tomcat tomcat/test`
      * `docker image tag tomcat/test <tag>`
      * `docker push <tag>`
    * troubleshoot:
      `docker exec -it tomcat bash` or `docker-compose exec tomcat bash`
      `apt-get update && apt-get install vim -y`
    * clean up:
      * `docker-compose down`
      * `docker system prune -f`
  * With `Dockerfile`: 
    1. Create network: `docker network create -d bridge mynetwork`
    1. Start db container.
    1. Connect db container to network: `docker network connect mynetwork <container> --alias db`
    1. Start API: `docker run -it --name tomcat --network mynetwork <image>`
    1. `docker build -t mapping-api .`
    1. `docker run --name tomcat -p 9080:8080 mapping-api:latest -d`
  * Without a container:
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
  