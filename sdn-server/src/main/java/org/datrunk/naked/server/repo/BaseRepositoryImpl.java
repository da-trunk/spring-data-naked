package org.datrunk.naked.server.repo;

import java.io.Serializable;
import javax.persistence.EntityManager;
import org.datrunk.naked.entities.CollectionDTO;
import org.datrunk.naked.entities.WithId;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

public class BaseRepositoryImpl<T extends WithId<ID>, ID extends Serializable>
    extends SimpleJpaRepository<T, ID> implements BaseRepository<T, ID> {

  protected final EntityManager entityManager;

  public BaseRepositoryImpl(
      JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
    super(entityInformation, entityManager);
    this.entityManager = entityManager;
  }

  @Override
  public Class<T> getDomainClass() {
    return super.getDomainClass();
  }

  /**
   * Query the DB and re-populate the entity. This will fail if the entity hasn't been persisted (id
   * = null) or doesn't exist in the DB. Call this after updating tables which could fire triggers
   * within the DB.
   *
   * @param entity to query re-populate. The entity's identifier must be initialized.
   */
  @Override
  public void refresh(T entity) {
    flush();
    if (entity.getId() != null) {
      final ID entityId = entity.getId();
      entity =
          findById(entity.getId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          String.format(
                              "Unable to refresh, entity with id [%s] was not found", entityId)));
      // entityManager.refresh(entity); // This requires that the entity be managed. It doesn't work
      // when running via Spring in
      // autocommit mode.
    } else {
      throw new IllegalArgumentException(
          String.format("Unable to refresh entity [%s]: id is uninitialized", entity));
    }
  }

  /**
   * Unlike save, this will modify the existing entity reference (updating a generated id and
   * changing state to managed).
   */
  @Transactional
  @Override
  public void persist(T entity) {
    entityManager.persist(entity);
  }

  @Transactional
  @Override
  public void detach(T entity) {
    entityManager.detach(entity);
  }

  @Transactional
  @Override
  public void remove(T entity) {
    entityManager.remove(entity);
  }

  /**
   * Query the DB and re-populate the entity before deleting it. Call this after updating tables
   * which could fire triggers within the DB.
   *
   * @param entity to query. The entity's identifier must be initialized.
   */
  @Transactional
  @Override
  public void refreshAndDelete(T entity) {
    if (entity.getId() != null) {
      entityManager.refresh(entity);
    } else {
      throw new IllegalArgumentException(
          String.format("Unable to refresh entity [%s]: id is uninitialized", entity));
    }
    entityManager.remove(entity);
  }

  @Transactional
  @Override
  public T saveAndRefresh(T entity) {
    entityManager.persist(entity);
    entityManager.flush();
    entityManager.refresh(entity);
    return entity;
  }

  @Transactional
  @Override
  // See
  // https://stackoverflow.com/questions/34031913/spring-data-rest-add-custom-endpoint-to-specific-reposiotry
  public void saveAll(CollectionDTO<T> entities) {
    for (T entity : entities.getEntities()) {
      entityManager.persist(entity);
    }
  }

  @Transactional
  @Override
  public void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
