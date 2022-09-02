package org.datrunk.naked.server.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

/**
 * A subset of JPARepository which is read only.
 *
 * @author da-trunk@outlook.com
 * @param <T>
 * @param <ID>
 */
@NoRepositoryBean
public interface ReadOnlyRepo<T, ID> extends Repository<T, ID>, QueryByExampleExecutor<T> {
  Optional<T> findById(Long id);

  boolean existsById(Long id);

  List<T> findAll();

  List<T> findAllById(Iterable<Long> ids);

  long count();

  T getById(Long id);

  List<T> findAll(Sort sort);

  Page<T> findAll(Pageable pageable);
}
