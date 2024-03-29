package org.datrunk.naked.server.repo;

import java.util.List;
import org.datrunk.naked.entities.Role;
import org.datrunk.naked.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface UserRepo<T extends User> extends BaseRepository<T, Integer> {
  List<T> findByRole(@Param("role") Role role, Pageable pageable);
}
