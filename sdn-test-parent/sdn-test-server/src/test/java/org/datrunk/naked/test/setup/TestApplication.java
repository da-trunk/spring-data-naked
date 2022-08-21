//package org.datrunk.naked.test.setup;
//
//import org.datrunk.naked.server.Application;
//import org.datrunk.naked.server.config.JerseyConfig;
//import org.glassfish.jersey.server.ResourceConfig;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//
///**
// * @Configuration for tests which want to import the full set of beans we use in
// *                production.
// * 
// * @author ansonator
// *
// */
//public class TestApplication extends Application {
//  private TestRestTemplate restTemplate = new TestRestTemplate();
//
//  @Override
//  public ResourceConfig jerseyConfig() {
//    JerseyConfig result = new JerseyConfig();
////        result.addProperties(ImmutableMap.of(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES.toString(), true));
//    return result;
//  }
//
//}
