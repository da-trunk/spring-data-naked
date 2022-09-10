
# Tips

## JSON logging

  * Oddly, `RestTemplate` and Bowman have no logging for the JSON payload before it is sent.  From the client side, `HttpEntityRequestCallback::logBody` comes close if you activate debug logging for channel `org.springframework.web.client.RestTemplate`.  To go further, set a breakpoint in `RestTemplate::doWithRequest(ClientHttpRequest httpRequest)` after the call to `genericConverter.write` near line 983.  The raw JSON is written to field `bufferedOutput` in the request.  When debugging a custom endpoint, this is very useful.  For example, to debug batch submits from Linux or WSL, copy the raw JSON to `<payload>` in command `curl -i -X POST -H "Content-Type:application/json" -d '<payload>' http://localhost:9080/api/batch`.  If you use PowerShell, you'll also need to use backslash to escape quotes inside the payload.
  * A failure to deserialize JSON in the server will often result in error before any of your code is invoked.  This is because SpringMVC typically employs annotations like `@RequestBody` to handle the deserialization before it is passed to your controller method.  I haven't found logging in Spring or Jackson for the raw JSON before it is deserialized.
  
## Generated identifiers
  
  * Database generated identifiers make it easy to end up with constraint violations.  For example, this happens when Spring repositories or EntityManager are persisting or merging given an object with a `null` id and a value already present in the DB for another column with a unique constraint.  In this case, the DB generates a new id and attempts the insert, but the DB fails the insert because another column is duplicating content.  It is easy for our random generators to do this because they are designed to reproduce the same random sequence each time.  So, anything not cleaned up after a test is likely to cause constraint violations the next time that test is run.  
	* Note: entities with natural ids can also have this problem if they do not load the set of ids present in the DB before generating a new value.
    
  
## Gateways
  
  * When the server is behind a Gateway, it will be necessary to use X-FORWARDED-* HEADERS to alter the URL in links.  See [Adding-HAL-Hypermedia-to-Spring-Boot-2-applications-using-Spring-HATEOAS](https://tech.asimio.net/2020/04/06/Adding-HAL-Hypermedia-to-Spring-Boot-2-applications-using-Spring-HATEOAS.html).