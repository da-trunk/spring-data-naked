FROM tomcat:9-jdk8-openjdk-buster
WORKDIR /usr/local/tomcat
COPY target/@project.artifactId@-@project.version@.war webapps/ROOT.war
COPY target/test-classes/conf/Catalina/localhost conf/Catalina/localhost
COPY target/test-classes/conf/* conf/
RUN for file in `ls conf/Catalina/localhost/*`; do sed -i 's/\r$//' $file; done
RUN sed -i 's/\r$//' conf/server.xml
ADD http://repo/content/com/oracle/ojdbc6_g/11.2.0.4/ojdbc6_g-11.2.0.4.jar lib/odbc6.jar
RUN mv webapps.dist/manager webapps
RUN apt-get update && apt-get install vim -y
EXPOSE 8080