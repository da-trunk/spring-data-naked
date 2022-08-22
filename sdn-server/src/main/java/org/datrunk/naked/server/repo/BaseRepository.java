package org.datrunk.naked.server.repo;

import java.io.Serializable;
import javax.persistence.EntityManager;
import org.datrunk.naked.entities.CollectionDTO;
import org.datrunk.naked.entities.WithId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * Enhances {@link JpaRepository} with methods which operate directly on the {@link EntityManager}.
 *
 * @author da-trunk@outlook.com
 * @param <T>
 * @param <ID>
 */
@NoRepositoryBean
public interface BaseRepository<T extends WithId<ID>, ID extends Serializable>
    extends JpaRepository<T, ID> {
  /**
   * Because the implementation of this interface inherits from {@link SimpleJpaRepository}, our
   * {@link #save} implementation calls {@link EntityManager#merge} for any {@link
   * javax.persistence.Entity} with a non-null @Id. {@link EntityManager#merge} returns the modified
   * Entity in a new reference. Classes with an externally assigned @Id (i.e. Vocabulary, Concept2,
   * ...) will have a non-null @Id at the time save is called, so they MUST replace their entity
   * instance with the new instance which save returns. Otherwise, I've encountered some odd
   * Hibernate / JPA behaviors downstream.
   *
   * <p>This method is useful when you know your Entity does not exist in the DB. It is a simple
   * pass through to {@link EntityManager#persist} and will immediately populate the Entity's
   * identifier when it can.
   *
   * @param entity A new entity which we want to insert into the DB.
   */
  void persist(T entity);

  void detach(T entity);

  /**
   * Recreates the entity from the data store.
   *
   * @param entity to query, it is modified to match the current DB state
   */
  void refresh(T entity);

  Class<T> getDomainClass();

  void remove(T entity);

  /**
   * Saves the provided entity, then returns a new proxy after querying the DB. This is useful when
   * the entity or its references are modified in the DB via triggers before they are persisted.
   * Simply saving the entity is insufficient because Hibernate's first level cache will avoid
   * re-querying the DB again.
   *
   * @param entity
   * @return The entity's proxy after persisting it and recreating it from the DB.
   */
  T saveAndRefresh(T entity);

  /**
   * Refreshes the provided entity from the data store, then deletes it. This is useful when
   * triggers or other DB operations have caused the entity to go out-of-sync with its memory
   * representation.
   *
   * @param entity
   */
  void refreshAndDelete(T entity);

  void flushAndClear();

  @RestResource(path = "saveAll", rel = "batchInsert")
  void saveAll(CollectionDTO<T> entities);
}
