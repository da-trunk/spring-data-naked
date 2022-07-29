# Spring Data Naked

Naked entities with spring-data-rest.

## Goals

The spring data naked framework facilitates rapid prototyping for simple RDBMS-backed RESTful services.  Through reflection, it avoid the layers of duplicated logic in a more traditional client-server-database stack.  This makes it easier to experiment with alternative schema designs.

The goal is to generate as much code as possible from a single set of annotated entities.  Following the technologies it is primarily built upon (Hibernate, Spring Data REST, and Jackson), all code is generated at runtime -- usually at application initialization.  The structure of the entities determines all.

## Features

	* Client, server, and persistence layer code is generated from a single set of annotated entities.  The single set of entities are referenced while generating code in these three different layers.
	* Client and server, and schema reference the same entities.  To accomplish this, individual annotations may be meaningful in only the client, only the server, or in both.
	* Efficient insert and update for every entity without the need for additional code.
	* Database schema can be generated from the same entities.  Currently, this is done with Hibernate and Liquibase.
	* Testing relies on dockerized containers.  These end-to-end tests 

## Implementation

Spring-data-naked is a low code framework built on top of the following technologies.

	* [bowman](https://github.com/hdpe/bowman) (java client)
	* [Jackson](https://github.com/FasterXML/jackson) (JSON serialization)
	* [Spring HATEOS](http://projects.spring.io/spring-hateoas/)
	* [Spring Data REST](https://spring.io/projects/spring-data-rest/)
	* [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
	* [Hibernate ORM](https://hibernate.org/orm/)
	* [Liquibase](https://www.liquibase.org/)
	
Testing relies on docker through [testcontainers](https://www.testcontainers.org/).  See [test](test/README.md).

