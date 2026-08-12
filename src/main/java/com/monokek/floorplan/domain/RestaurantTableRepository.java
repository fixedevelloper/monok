package com.monokek.floorplan.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface RestaurantTableRepository extends Repository<RestaurantTable, Long> {

    RestaurantTable save(RestaurantTable table);

    Optional<RestaurantTable> findById(Long id);

    void deleteById(Long id);

    List<RestaurantTable> findAll();

    List<RestaurantTable> findByFloorId(Long floorId);

    List<RestaurantTable> findAllByOrderByNameAsc();

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByName(String name);
}
