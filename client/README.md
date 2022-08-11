# Running tests manually

  * Start database
  * Start the server: `mvn spring-boot:run -pl si-job-server
  * Edit `application-test.yml` to point to the existing database and service.
  * Run tests
  
# Migrating the database

  * Generate change sets from the entities: `mvn liquibase:diff`
  * Deploy change sets to the empty db: `mvn liquibase:update`
    
# Tips

  * Oddly, `RestTemplate` and Bowman have no logging for the JSON payload before it is sent.  From the client side, `HttpEntityRequestCallback::logBody` comes close if you activate debug logging for channel `org.springframework.web.client.RestTemplate`.  To go further, set a breakpoint in `RestTemplate::doWithRequest(ClientHttpRequest httpRequest)` after the call to `genericConverter.write` near line 983.  The raw JSON is written to field `bufferedOutput` in the request.  When debugging a custom endpoint, this is very useful.  For example, to debug batch submits from Linux or WSL, copy that to `<payload>` in command `curl -i -X POST -H "Content-Type:application/json" -d '<payload>' http://localhost:9080/api/batch`.  If you use Powershell, you'll also need to use backslash to escape quotes inside the payload.
  * A failure to deserialize JSON in the server will often result in error before any of your code is invoked.  This is because SpringMVC typically employs annotations like `@RequestBody` to handle the deserialization before it is passed to your controlled method.  I haven't found logging in Spring or Jackson for the raw JSON before it is deserialized.
  * Database generated identifiers make it easy to end up with constraint violations.  For example, this happens when Spring repositories or EntityManager are persisting or merging given an object with a `null` id and a value already present in the DB for another column with a unique constraint.  In this case, the DB generates a new id and attempts the insert, but the DB fails the insert because another column is duplicating content.  It is easy for our random generators to do this because they are designed to reproduce the same random sequence each time.  So, anything not cleaned up after a test is likely to cause constraint violations the next time that test is run.  
    * Note: entities with natural ids can also have this problem if they do not load the set of ids present in the DB before generating a new value.
  * `JSON parse error: Could not resolve type id 'com.cerner.si.mapping.entities.legacy.Consumer_$$_jvst67_0' as a subtype of `com.cerner.si.client.entities.WithId`: no such class found`.  This will happen any time we send an object which we obtained via a get request to `BowmanClient`.  If you strip the `_$$_jvst67_0` from the JSON payload's `@class` fields, things will work.  We need to figure out a way around this issue.  See the `@JsonTypeInfo` annotation in the `CollectionDTO` class.
  * When the server is behind the Gateway, it will be necessary to use X-FORWARDED-* HEADERS to alter the URL in links.  See [this](https://tech.asimio.net/2020/04/06/Adding-HAL-Hypermedia-to-Spring-Boot-2-applications-using-Spring-HATEOAS.html).