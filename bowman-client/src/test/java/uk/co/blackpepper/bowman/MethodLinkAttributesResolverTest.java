package uk.co.blackpepper.bowman;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;

import org.datrunk.naked.entities.bowman.annotation.LinkedResource;
import org.junit.Before;
import org.junit.Test;

public class MethodLinkAttributesResolverTest {
	
	private interface Content {
		
		@LinkedResource
		Object getNoRel();
		
		@LinkedResource(rel = "custom")
		void withRel();
		
		@LinkedResource(optionalLink = false)
		Object getNotOptional();
		
		@LinkedResource(optionalLink = true)
		Object getOptional();
	}
	
	private MethodLinkAttributesResolver resolver;
	
	@Before
	public void setUp() {
		resolver = new MethodLinkAttributesResolver();
	}
	
	@Test
	public void resolveForMethodWithNoRelReturnsMethodNameAsLinkName() throws Exception {
		Method linked = Content.class.getMethod("getNoRel");
		
		MethodLinkAttributes attribs = resolver.resolveForMethod(linked);
		
		assertThat(attribs.getLinkName(), is("noRel"));
	}
	
	@Test
	public void resolveForMethodWithRelReturnsRelAsLinkName() throws Exception {
		Method linked = Content.class.getMethod("withRel");
		
		MethodLinkAttributes attribs = resolver.resolveForMethod(linked);
		
		assertThat(attribs.getLinkName(), is("custom"));
	}
	
	@Test
	public void resolveForMethodWithDefaultOptionalLinkReturnsOptionalIsFalse() throws Exception {
		Method linked = Content.class.getMethod("withRel");
		
		MethodLinkAttributes attribs = resolver.resolveForMethod(linked);
		
		assertThat(attribs.isOptional(), is(false));
	}
	
	@Test
	public void resolveForMethodWithOptionalLinkFalseReturnsOptionalIsFalse() throws Exception {
		Method linked = Content.class.getMethod("getNotOptional");
		
		MethodLinkAttributes attribs = resolver.resolveForMethod(linked);
		
		assertThat(attribs.isOptional(), is(false));
	}
	
	@Test
	public void resolveForMethodWithOptionalLinkTrueReturnsOptionalIsTrue() throws Exception {
		Method linked = Content.class.getMethod("getOptional");
		
		MethodLinkAttributes attribs = resolver.resolveForMethod(linked);
		
		assertThat(attribs.isOptional(), is(true));
	}
}
