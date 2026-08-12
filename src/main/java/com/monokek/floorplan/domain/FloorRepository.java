package com.monokek.floorplan.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface FloorRepository extends Repository<Floor, Long> {

    Floor save(Floor floor);

    Optional<Floor> findById(Long id);

    void deleteById(Long id);

    List<Floor> findAll();

    List<Floor> findByBranchId(Long branchId);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByName(String name);
}
