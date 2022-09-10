# Testing Philosophy

The Spring Data Naked implementation relies heavily on well-tested external projects.  But, the nature of these underlying projects enable a wide variety of use cases.  When combining that with the wide variety of server and RDBMS implementations, it is not uncommon for unknown problems to be exposed.  Spring-data-naked's dependencies generate a lot of dynamic code at runtime and may not be well tested with a specific database or server implementation.

The projects here provide code to start and stop docker containers for some popular server and database implementations.  Builds from this project exercise JUnit5 end-to-end tests with these containers.  This can help to flush out issues with specific server or database implementations.  

Instead of assuming that the underlying components will work with your entity, you can use this project test them.  When you find and fix issues, please submit a PR which adds your entities to the test-entities project and expose the problem you found.  Even if you don't have a solution, it will be very help to expose the problem with a `@Disabled` test.

There is not a lot of actual code in spring-data-rest.  Most of it is configuration and usage patterns.  Batch inserts are the one exception to this.  For them, there are some traditional JUnit5 tests using mocks.

## Server Tests

This currently only supports testing with Tomcat.  See [sdn-test-server-tomcat](sdn-test-server-tomcat/).

## Database Tests

See [sdn-test-db-parent](sdn-test-db-parent/)

## Implementation

Spring Data Naked tests [testcontainers](https://www.testcontainers.org/) and Spring to start and stop docker containers during JUnit5 tests.  See [sdn-test-common](sdn-test-common/) and [sdn-test-db-common](sdn-test-db-parent/sdn-test-db-common/) for useful classes when adding test functionality for new server and RDBMS implmentations.
