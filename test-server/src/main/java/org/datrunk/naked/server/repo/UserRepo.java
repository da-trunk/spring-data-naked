package org.datrunk.naked.server.repo;

import java.util.List;

import org.datest.naked.test.entities.Role;
import org.datest.naked.test.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface UserRepo<T extends User> extends BaseRepository<T, Long> {// ReadOnlyRepo<T, Long> {
    List<T> findByRole(@Param("role") Role role, Pageable pageable);
}
