This simple demo consists of the following primary components:
	* test-entities
	* test-server
	* test-client
		
It is a port of [.NET 6.0: Code First with Entity Framework Core and MySQL](https://www.daveops.co.in/post/code-first-entity-framework-core-mysql) to the spring-data-naked framework with JDK8.  It also adds random data generators based on Faker in order to simplify test data generation.

The walkthrough below was copied from [.NET 6.0: Code First with Entity Framework Core and MySQL](https://www.daveops.co.in/post/code-first-entity-framework-core-mysql), then modified to set up the same demo with spring-data-naked.

# Walkthrough

Spring Data Naked (SDN) relies on Hibernate for mapping entities with database objects.  In addition, SDN provides easy to use functions which eliminate the need for writing most code.  SDN also provides command line tools to generate database migrations in various RDBMS platforms.

## What is Code First? 

With Code First approach you can create entities or models in your code. Various attributes and configurations defines the relationships and keys in the database. Once the models are defined and configured, we can migrate them to the database using Liquibase CLI tools.  

In order to better understand the code first approach it's always better to have a practical example.  We will create two entities User & Job.  User will have a primary key Id and FirstName as string. Job entity will have a primary key Id, Name as string and UserId as foreign key to the User entity. 


## Prerequisites 

Before we start we need to install a Java 8+ runtime and Maven.  You will also need a connection to the internet.  While building with maven, the tests will start a MySQL docker container on port 3306.  This port will be mapped to a randomly determined available port on the host.  

## Setting Up The Project 

Use your favorite IDE to create a new Java Maven project.  It should consist of three modules:

* test-entities
* test-server

Copy the `pom.xml` file for each of these modules from the equivalently named modules here.

Create `src/main/java/User.java` as below. 

```java
@Entity
@RemoteResource("/users")
@Getter
@Setter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends IdClass<Integer> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(insertable = false, updatable = false)
  private Integer id;

  private String firstName;
}
```  

We will also need a BaseRepository subclass to access these entities. Create a file name `UserRepo.java` and add the below content.  

```java
@RepositoryRestResource
public interface UserRepo<User> extends BaseRepository<Integer> {
}
```

To configure the database connection, create a file at `test-server/src/main/resources/application.yml` with the following.

```
server:
  tomcat.accesslog:
    enabled: true
    basedir: target/tomcat
  port: '9080'
management:
  endpoints:
    web:
      base-path: /actuator
      exposure.include: '*'
  endpoint:
    shutdown.enabled: true
spring:
  config:
    import:
    - classpath:application-server.yml
    - classpath:mysql.yml
  liquibase:
    enabled: false
  datasource:
    url: jdbc:mysql://localhost:3306/test
    username: test
    password: password
```

## Migrations

We are all set code-wise. But we still need to setup our database.  In order to do so we will create a User and Job table in our test database.

The combination of Hibernate and Liquibase provides a very convenient way to create a migration of our entities which will then be pushed to our database. 

1. Start an empty docker DB
  1. Pull image: `docker pull <image>`
  1. Start container: `docker run --shm-size=1g --name test-db-oracle -p 1521:1521 -e ORACLE_SID=oracle -e ORACLE_PDB=oracle -e ORACLE_PWD=password <image>`
1. Rebuild: `mvn install -pl "test/test-entities,test/test-db/test-db-mysql" -DskipTests`
1. Generate change sets from the entities: `mvn liquibase:diff -pl test/test-db/test-db-mysql`
1. Find what was generated: `ls -lrt test/test-db/test-db-mysql/src/main/resources/liquibase/migration_*.xml | head -1`
1. Generate migration SQL: `mvn liquibase:updateSQL -pl test/test-db/test-db-mysql -D "liquibase.changeLogFile=schema_${maven.build.timestamp}.xml"`
1. View migration SQL: `cat test/test-db/test-db-mysql/target/liquibase/migrate.sql`
1. Apply update: `mvn liquibase:update -pl test/test-db/test-db-mysql -D "liquibase.changeLogFile=migration_${maven.build.timestamp}.xml"`

The above statements once executed will create the database, the tables within, and also setup the relationship between the two tables. 

## Adding Data to MySQL 

In order to fetch data from the database we first need to add data in the tables. Install any MySQL client to connect to the database. My personal favorite is MySQL Workbench.  Now, you can add data from DBeaver by first adding a connection with details like Host=`localhost`, Port=`3306`, User=`test` & Password=`password`.  

Once connected navigate to User table and add a row and save the data. Similarly, navigate to the Job table, add a row and save the data. Our database is all set now. Let’s run our project and view the results.  

## Putting Everything Together 

Go back to your project and run the below command to start our project.  

`mvn spring-boot:run -pl test-server`

The above command will start your project.  Now, open a browser and navigate to http://localhost:9080/api/users.  A JSON response with the first page of data from table `users` will be displayed.  This is the GET request to retrieve all users. 

For an graphical API, navigate to http://localhost:9080/api.  This makes it easier to test POST, PATCH, and DELETE end points.

## Adding a client

You now have a server with end points to display, create, update, and delete your User and Job entities.  If you would also like Java methods which you can call to perform these actions, do the following.

1. Create a third module named `test-client`.  Set up its `pom.xml` by copying from the example in this project.
1. Create the client for User.

```java
CEClient.Factory repoClientFactory = new CEClient.Factory(properties);
RepoClient<User, Integer> userClient = repoClientFactory.create(User.class, Long.class);
```

1. Now, you can retrieve the list of Users via a method call: `List<User> users = userClient.getAll();`
1. See `RepoClient` for a list of methods.

## Adding an optional field

Now, let's try adding a new optional field to `User`.  We'll add the field `lastName` by adding the following lines to `User.java`.

```java
private String lastName;
```

Run Liquibase to generate SQL commands which will add this new column to the proper database table:

```
mvn liquibase:diff
mvn liquibase:updateSQL
```

Rebuild, then start the server.

```
mvn package
mvn spring-boot:run -pl test-server
```

## Adding a new entity

We will add a new entity named Job.  Each Job entity will reference either zero or one instance of User.

```java
@Entity
@RemoteResource("/jobs")
@Getter
@Setter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job extends IdClass<Integer> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(insertable = false, updatable = false)
  private Integer id;

  @Nonnull
  private String name;

  @OneToOne
  private User user;
}
```

Next we will create a GET API which for Job objects in the database. 

```java
@RepositoryRestResource
interface JobRepo extends ReadOnlyRepo<Job, Integer> {
}
```

As before, migrate the database and restart the server.


## Conclusion 

To conclude, SDN is one of the best O/RM I have encountered and works very well with a lot of different databases.  Setting it up is as easy as firing a few commands.  The migrations from entities to database is super smooth.  

Hope you have enjoyed the content.