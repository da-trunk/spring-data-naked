## Goals

Spring Data Naked relies heavily on end-to-end testing via docker containers.  The goal is to exercise as much of the dynamically generated code in each component as possible.  Components like Hibernate, Jackson, and Spring Data Rest are widely used outside this project and are well tested.  But, they also support an extremely wide variety of use cases.  With a given RDBMS, end-to-end testing sometimes exposes limitations or complexities in use cases which Spring Data Naked is unable to properly support.

## Implementation

Spring Data Naked testing relies on docker through [testcontainers](https://www.testcontainers.org/).  See [test](test/README.md).
