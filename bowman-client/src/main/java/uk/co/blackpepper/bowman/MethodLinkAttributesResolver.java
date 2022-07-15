package uk.co.blackpepper.bowman;

import static uk.co.blackpepper.bowman.HalSupport.toLinkName;

import java.lang.reflect.Method;

import org.datrunk.naked.entities.bowman.annotation.LinkedResource;

class MethodLinkAttributesResolver {
	
	MethodLinkAttributes resolveForMethod(Method method) {
		LinkedResource annotation = method.getAnnotation(LinkedResource.class);
		
		String rel = annotation.rel();
		
		if ("".equals(rel)) {
			rel = toLinkName(method.getName());
		}
		
		return new MethodLinkAttributes(rel, annotation.optionalLink());
	}
}
