package uk.co.blackpepper.bowman;

import java.lang.reflect.Method;
import java.net.URI;
import org.datrunk.naked.entities.WithUri;
import org.datrunk.naked.entities.bowman.annotation.ResourceId;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;

class ResourceIdMethodHandler implements ConditionalMethodHandler {

  private final EntityModel<?> resource;

  ResourceIdMethodHandler(EntityModel<?> resource) {
    this.resource = resource;
  }

  @Override
  public boolean supports(Method method) {
    return method.isAnnotationPresent(ResourceId.class);
  }

  @Override
  public Object invoke(Object self, Method method, Method proceed, Object[] args) {
    URI selfLink =
        resource
            .getLink(IanaLinkRelations.SELF)
            .map(link -> URI.create(link.getHref()))
            .orElse(null);
    if (self instanceof WithUri) {
      ((WithUri) self).setUri(selfLink);
    }
    return selfLink;
  }
}
