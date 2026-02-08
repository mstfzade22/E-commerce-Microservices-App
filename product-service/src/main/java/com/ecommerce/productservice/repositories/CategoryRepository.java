package com.ecommerce.productservice.repositories;

import com.ecommerce.productservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentIsNull();

    List<Category> findByParentId(Long parentId);

    List<Category> findAllByParentId(Long parentId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent LEFT JOIN FETCH c.children WHERE c.id = :id")
    Optional<Category> findByIdWithParentAndChildren(@Param("id") Long id);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent LEFT JOIN FETCH c.children WHERE c.slug = :slug")
    Optional<Category> findBySlugWithParentAndChildren(@Param("slug") String slug);
}
