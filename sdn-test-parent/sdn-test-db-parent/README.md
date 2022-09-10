# sdn-test-db-parent

These project start databases for SDN test and demo purposes.  They serve two purposes:

* Tests depend on this project to pull in a base properties configuration and a testcontainers class that is useful when starting a test database.
* These projects include an executable to assist in starting a test database for demo purposes.

# Quick Start

1. Start a MySQL database: `mvn spring-boot:run -pl ./sdn-test-parent/sdn-test-db-parent/sdn-test-db-mysql`.  The DB is started at `jdbc:mysql://localhost:3306/test` with user/login `test/password`.
1. Start an Oracle 21c XE database: `mvn spring-boot:run -pl ./sdn-test-parent/sdn-test-db-parent/sdn-test-db-oracle`.  The DB is started at `jdbc:mysql://localhost:1521/XEPDB1` with user/login `test/password`.
