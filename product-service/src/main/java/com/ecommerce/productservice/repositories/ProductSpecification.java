package com.ecommerce.productservice.repositories;

import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.StockStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.equal(root.get("isActive"), true);
    }

    public static Specification<Product> hasKeyword(String keyword) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) ->
                cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> isFeatured() {
        return (root, query, cb) -> cb.equal(root.get("isFeatured"), true);
    }

    public static Specification<Product> priceRange(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (min != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), min));
            }
            if (max != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), max));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> hasStockStatus(String status) {
        return (root, query, cb) ->
                cb.equal(root.get("stockStatus"), StockStatus.valueOf(status));
    }

    public static Specification<Product> hasAttribute(String key, String value) {
        return (root, query, cb) ->
                cb.equal(
                        cb.function("jsonb_extract_path_text", String.class,
                                root.get("attributes"), cb.literal(key)),
                        value
                );
    }

    public static Specification<Product> buildFilter(
            String keyword, Long categoryId, Boolean featured,
            BigDecimal minPrice, BigDecimal maxPrice,
            String stockStatus, Map<String, String> attributes) {

        Specification<Product> spec = isActive();

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(hasKeyword(keyword));
        }
        if (categoryId != null) {
            spec = spec.and(hasCategory(categoryId));
        }
        if (Boolean.TRUE.equals(featured)) {
            spec = spec.and(isFeatured());
        }
        if (minPrice != null || maxPrice != null) {
            spec = spec.and(priceRange(minPrice, maxPrice));
        }
        if (stockStatus != null && !stockStatus.isBlank()) {
            spec = spec.and(hasStockStatus(stockStatus));
        }
        if (attributes != null && !attributes.isEmpty()) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                spec = spec.and(hasAttribute(entry.getKey(), entry.getValue()));
            }
        }

        return spec;
    }
}
