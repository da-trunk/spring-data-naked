# Spring Data Naked

[Naked objects](https://en.wikipedia.org/wiki/Naked_objects) with [spring-data-rest](https://spring.io/projects/spring-data-rest).

## Goals

The spring data naked framework facilitates rapid prototyping for simple RDBMS-backed RESTful services.  Through reflection, it avoid the layers of duplicated logic in a more traditional client-server-database stack.  This makes it easier to experiment with alternative schema designs.

Spring-data-naked goes further by extending the entity-driven dynamic model to the top-most layer (client) and the bottom-most layer (database schema migrations).

The goal is to generate as much code as possible from a single set of annotated entities.  Following the technologies it is primarily built upon (Hibernate, Spring Data REST, and Jackson), all code is generated at runtime -- usually at application initialization.  The structure of the entities determines all.

## Features

* Client, server, and persistence layer code is generated from a single set of annotated entities.
* Client, server, and schema can reference the exact same entities code.  To accomplish this, individual annotations may be meaningful in only the client, only the server, or in both.
	* It is recommended to generate database migrations from the entities.  In examples, this is done with [Hibernate ORM](https://hibernate.org/orm/) and [Liquibase](https://www.liquibase.org/).
* JDBC batch insert for every entity without the need for additional code.
* Testing relies on dockerized containers.  These end-to-end tests exercise the dynamically generated code against a variety of database vendors. 

## Implementation

Spring-data-naked is a low-code framework built on top of the following technologies.

* A specialized copy of [bowman](https://github.com/hdpe/bowman) (java client)
* [Jackson](https://github.com/FasterXML/jackson) (JSON serialization)
* [Spring HATEOS](http://projects.spring.io/spring-hateoas/)
* [Spring Data REST](https://spring.io/projects/spring-data-rest/)
* [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
* [Hibernate ORM](https://hibernate.org/orm/)
* [Liquibase](https://www.liquibase.org/)
	
Testing relies on docker through [testcontainers](https://www.testcontainers.org/).  See [sdn-test-parent](sdn-test-parent/README.md).

