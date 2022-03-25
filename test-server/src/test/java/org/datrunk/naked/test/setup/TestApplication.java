package org.datrunk.naked.test.setup;


import org.datrunk.naked.server.config.Application;
import org.datrunk.naked.server.config.JerseyConfig;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * @Configuration for tests which want to import the full set of beans we use in production.
 * 
 * @author ansonator
 *
 */
public class TestApplication extends Application {
    @Override
    public ResourceConfig jerseyConfig() {
        JerseyConfig result = new JerseyConfig();
//        result.addProperties(ImmutableMap.of(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES.toString(), true));
        return result;
    }
}
