# Create Database

```sh
sqlplus sys/password@localhost as sysdba
SQL> ALTER SESSION SET CONTAINER=XEPDB1;
SQL> CREATE USER TEST IDENTIFIED BY password QUOTA UNLIMITED ON USE
SQL> GRANT CONNECT, RESOURCE, CREATE SESSION, create table TO TEST;
SQL> quit
sqlplus test/test@//localhost/XEPDB1
```

# Compare entities to dev and generate changeset to migrate dev to entities

`mvn liquibase:diff`
  
  * This may include drop statements for DB objects not represented in the entities.  See next sections to remove those.
  * Before generating your migration, be sure to delete the migration output file if it already exists.  If you don't, the new commands will be appended to it.

# Exclude objects from the diff

  Note: there is already a pattern matcher in `pom.xml`.  This matches tables which start with a lower case letter.  It also matches dependencies of those tables.  This was done to include our JPA entities without including anything that we haven't modeled as an entity.

  * If the `<diffIncludeObject>` in the `pom.xml` doesn't work, you may need to comment it out and do things manually.  See examples below:
    * Only include consumer and task tables and their dependent objects: `mvn liquibase:diff -D 'liquibase.diffIncludeObjects="table:consumer.*,table:.*task.*"'`
    * Exclude objects with name starting with consumer regardless of case.  Also exclude dependencies of these tables: `mvn liquibase:diff -D 'liquibase.diffExcludeObjects="(i?)consumer.*"'`
    * See [documentation](https://docs.liquibase.com/workflows/liquibase-community/including-and-excluding-objects-from-a-database.html) for more examples.
  
# Remove all drop statements from an XML changeset

  If inclusions / exclusion don't work.

  * Open *Notepad++*
  * Search and replace:
    * Search Mode = *Regular expression*
    * search: `<changeSet[^>]*>(?s)\s+<drop\w+ ([^>]+?)>\s+<\/changeSet>\s+(?-s)`
    * replace:
    
