package org.datrunk.naked.server.repo;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.extern.log4j.Log4j2;
import org.datrunk.naked.entities.CollectionDTO;
import org.datrunk.naked.entities.WithId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@BasePathAwareController // Why does @RepositoryRestController not work?
@ResponseBody
@ExposesResourceFor(CollectionDTO.class)
@Transactional(value = TxType.REQUIRED)
@Log4j2
public class BatchRestRepo {
  @PersistenceContext private EntityManager em;
  private final EntityLinks entityLinks;

  @Autowired
  public BatchRestRepo(EntityLinks entityLinks) {
    this.entityLinks = entityLinks;
  }

  /**
   * {@link EntityManager#merge merge} every object in the provided collection.
   *
   * @param dto the collection to persist, wrapped in {@link CollectionDTO}.
   * @return the persisted collection, wrapped in {@link CollectionModel}.
   */
  @PostMapping(value = "/batch")
  public ResponseEntity<CollectionModel<EntityModel<? extends WithId>>> saveAll(
      @RequestBody EntityModel<CollectionDTO<? extends WithId>> dto) {
    List<? extends WithId> entities = Objects.requireNonNull(dto.getContent()).getEntities();
    List<? extends WithId> mergedEntities =
        entities.stream().map(em::merge).collect(Collectors.toList());
    em.flush();

    ArrayList<EntityModel<? extends WithId>> resources =
        mergedEntities.stream()
            .map(
                entity -> {
                  Link selfLink =
                      entityLinks
                          .linkToItemResource(entity.getClass(), entity.getId())
                          .withSelfRel();
                  EntityModel<? extends WithId> resource = EntityModel.of(entity).add(selfLink);
                  List<Link> links =
                      Stream.of(entity.getClass().getMethods())
                          .filter(BatchRestRepo::isEntityGetter)
                          .map(getter -> toLink(entity, getter))
                          .filter(link -> link != null)
                          .peek(link -> log.debug("adding link {} to {}", link, entity.getClass()))
                          .collect(Collectors.toList());
                  return EntityModel.of(entity, links);
                })
            .collect(Collectors.toCollection(ArrayList::new));
    return new ResponseEntity<>(CollectionModel.of(resources), HttpStatus.CREATED);
  }

  private static boolean isEntityGetter(Method method) {
    final int modifiers = method.getModifiers();
    if (Modifier.isStatic(modifiers)) return false;
    if (!Modifier.isPublic(modifiers)) return false;
    if (!method.getName().startsWith("get")) return false;
    if (method.getParameterCount() != 0) return false;
    final Class<?> possibleEntityType;
    if (Collection.class.isAssignableFrom(method.getReturnType())) {
      Type genericReturnType = method.getGenericReturnType();
      if (genericReturnType instanceof ParameterizedType) {
        Type type =
            ((java.lang.reflect.ParameterizedType) genericReturnType).getActualTypeArguments()[0];
        assert (type instanceof Class);
        possibleEntityType = (Class<?>) type;
      } else return false;
    } else {
      possibleEntityType = method.getReturnType();
    }
    if (possibleEntityType.getAnnotation(Entity.class) == null) return false;
    return true;
  }

  private Link toLink(final WithId<?> entity, Method getter) {
    final Link propertyLink;
    if (Collection.class.isAssignableFrom(getter.getReturnType())) {
      Type genericReturnType = getter.getGenericReturnType();
      if (genericReturnType instanceof ParameterizedType) {
        Type type =
            ((java.lang.reflect.ParameterizedType) genericReturnType).getActualTypeArguments()[0];
        assert (type instanceof Class);
        Class<?> linkedEntityType = (Class<?>) type;
        propertyLink = entityLinks.linkToCollectionResource(linkedEntityType);
      } else {
        return null;
      }
    } else {
      Class<?> linkedEntityType = getter.getReturnType();
      try {
        final Object val = getter.invoke(entity);
        if (val instanceof WithId<?>) {
          final WithId<?> linkedEntity = (WithId<?>) val;
          propertyLink = entityLinks.linkToItemResource(linkedEntityType, linkedEntity.getId());
        } else return null;
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        log.catching(e);
        return null;
      }
    }
    Link link =
        entityLinks
            .linkFor(entity.getClass())
            .slash(entity.getId())
            .slash(propertyLink.getRel())
            .withRel(propertyLink.getRel());
    return link;
  }

  private static Class<?> getEntityTypeFromProxy(Object proxy) {
    Class<?> parentType = proxy.getClass().getSuperclass();
    if (parentType.getAnnotation(Entity.class) != null) {
      return parentType;
    }
    return null;
  }

  private static boolean isSubTypeOf(Class<?> type, Class<?> parent) {
    while (!type.equals(parent) && !type.equals(Object.class)) {
      return isSubTypeOf(type.getSuperclass(), parent);
    }
    return type.equals(parent);
  }

  private static boolean isSuperTypeOf(Class<?> superType, Class<?> subType) {
    return isSubTypeOf(subType, superType);
  }

  /**
   * {@link EntityManager#persist persist} every object in the provided collection.
   *
   * @param dto the collection to persist, wrapped in {@link CollectionDTO}.
   * @return the persisted collection, wrapped in {@link CollectionModel}.
   */
  @PostMapping(value = "/persist")
  public ResponseEntity<List<?>> persist(@RequestBody EntityModel<CollectionDTO<?>> dto)
      throws IOException {
    List<?> entities = Objects.requireNonNull(dto.getContent()).getEntities();
    entities.stream().forEach(em::persist);
    em.flush();
    return new ResponseEntity<>(HttpStatus.CREATED);
  }
}
