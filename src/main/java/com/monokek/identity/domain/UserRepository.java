package com.monokek.identity.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port: the domain/application layer depends on this interface
 * only. {@code identity.infrastructure.persistence.JpaUserRepository}
 * supplies the actual JPA-backed implementation. Deliberately narrower than
 * {@code JpaRepository} — no {@code flush()}, {@code getReferenceById()} or
 * batch-delete methods leaking persistence-technology concerns into the domain.
 */
@NoRepositoryBean
public interface UserRepository extends Repository<User, Long> {

    User save(User user);

    Optional<User> findById(Long id);

    List<User> findAll();

    Optional<User> findByEmail(String email);

    Optional<User> findByUuid(UUID uuid);

    boolean existsByEmail(String email);

    List<User> findByIdIn(Collection<Long> ids);
}
