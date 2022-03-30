FROM tomcat:9-jdk8-openjdk-buster
WORKDIR /usr/local/tomcat
COPY target/@project.artifactId@-@project.version@.war webapps/ROOT.war
COPY target/test-classes/conf/Catalina/localhost conf/Catalina/localhost
COPY target/test-classes/conf/* conf/
RUN for file in `ls conf/Catalina/localhost/*`; do sed -i 's/\r$//' $file; done
RUN sed -i 's/\r$//' conf/server.xml
ADD https://repo.maven.apache.org/maven2/com/oracle/database/jdbc/ojdbc8/21.5.0.0/ojdbc8-21.5.0.0.jar
RUN mv webapps.dist/manager webapps
RUN apt-get update && apt-get install vim -y
EXPOSE 8080