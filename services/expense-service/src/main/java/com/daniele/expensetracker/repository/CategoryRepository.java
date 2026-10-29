package com.daniele.expensetracker.repository;

import com.daniele.expensetracker.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByOrderByDisplayNameAsc();

    Optional<Category> findByCodeIgnoreCase(String code);

    Optional<Category> findByDisplayNameIgnoreCase(String displayName);

    boolean existsByCodeIgnoreCase(String code);

    void deleteByCodeIgnoreCase(String code);
}
