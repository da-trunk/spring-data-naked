package uk.co.blackpepper.bowman;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.datrunk.naked.entities.bowman.annotation.RemoteResource;
import org.junit.Test;

public class ConfigurationTest {
	
	@RemoteResource("/y")
	private static class Entity {
	}
	
	@Test
	public void buildClientFactoryBuildsFactoryWithConfiguration() {
		ClientFactory factory = Configuration.builder()
			.setBaseUri(URI.create("http://x.com")).build().buildClientFactory();
		
		Client<Entity> client = factory.create(Entity.class);
		
		assertThat(client.getBaseUri(), is(URI.create("http://x.com/y")));
	}
}
