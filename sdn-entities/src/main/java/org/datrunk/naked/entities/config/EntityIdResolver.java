package org.datrunk.naked.entities.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import java.io.IOException;
import javax.persistence.Entity;

public class EntityIdResolver extends TypeIdResolverBase {
  private JavaType containerType;

  @Override
  public void init(JavaType baseType) {
    containerType = baseType;
  }

  @Override
  public Id getMechanism() {
    return Id.CLASS;
  }

  @Override
  public String idFromValue(Object obj) {
    return idFromValueAndType(obj, obj.getClass());
  }

  @Override
  public String idFromValueAndType(Object obj, Class<?> subType) {
    Class<?> type = subType;
    while (type != Object.class && type.getAnnotation(Entity.class) == null) {
      type = type.getSuperclass();
    }
    if (type != Object.class) {
      return type.getTypeName();
    } else {
      return getClass().getTypeName();
    }
  }

  @Override
  public JavaType typeFromId(DatabindContext context, String id) throws IOException {
    Class<?> subType;
    try {
      subType = Class.forName(id);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    return context.constructType(subType);
  }
}
