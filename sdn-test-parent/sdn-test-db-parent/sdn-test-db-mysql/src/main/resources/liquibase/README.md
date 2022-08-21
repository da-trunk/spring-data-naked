# Compare entities to dev and generate changeset to migrate dev to entities

	* Generate changelog: `mvn liquibase:diff`
		* By default, the changelog is written to `target/liquibase/schema_${maven.build.timestamp}.xml`
		* This may include drop statements for DB objects not represented in the entities.  See next sections to remove those.
		* Before generating your migration, be sure to delete the migration output file if it already exists.  If you don't, the new commands will be appended to it.
	* Generate schema file: `mvn liquibase:updateSQL -D "target/liquibase/schema_20220710011102.xml"`
		* By default, this is written to `target/liquibase/migrate.sql`
	* Apply schema: `mvn liquibase:update -D "target/liquibase/schema_20220710011102.xml"`

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
    
