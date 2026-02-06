package com.ecommerce.productservice.service;

import com.ecommerce.productservice.config.RedisConfig;
import com.ecommerce.productservice.dto.request.CreateCategoryRequest;
import com.ecommerce.productservice.dto.request.UpdateCategoryRequest;
import com.ecommerce.productservice.dto.response.CategoryCreateResponse;
import com.ecommerce.productservice.dto.response.CategoryDetailResponse;
import com.ecommerce.productservice.dto.response.CategorySummaryResponse;
import com.ecommerce.productservice.dto.response.CategoryTreeResponse;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.exception.DuplicateResourceException;
import com.ecommerce.productservice.exception.InvalidRequestException;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.mapper.CategoryMapper;
import com.ecommerce.productservice.repositories.CategoryRepository;
import com.ecommerce.productservice.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductRepository productRepository;

    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.CATEGORY_TREE, allEntries = true)
    public CategoryCreateResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating category with slug: {}", request.slug());

        if (categoryRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Category", "slug", request.slug());
        }

        Category category = categoryMapper.toEntity(request);

        if (request.parentId() != null) {
            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", request.parentId()));
            category.setParent(parent);
        }

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", savedCategory.getId());

        return categoryMapper.toCreateResponse(savedCategory);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CacheNames.CATEGORY_BY_SLUG, allEntries = true),
            @CacheEvict(value = RedisConfig.CacheNames.CATEGORY_TREE, allEntries = true)
    })
    @CachePut(value = RedisConfig.CacheNames.CATEGORY_BY_ID, key = "#id")
    public CategoryDetailResponse updateCategory(Long id, UpdateCategoryRequest request) {
        log.info("Updating category with ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (request.getSlug() != null && !request.getSlug().equals(category.getSlug())) {
            if (categoryRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
                log.warn("Update failed - Slug already exists: {}", request.getSlug());
                throw new DuplicateResourceException("Category", "slug", request.getSlug());
            }
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new InvalidRequestException("Category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", request.getParentId()));
            category.setParent(parent);
        }

        categoryMapper.updateEntityFromRequest(request, category);

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully with ID: {}", updatedCategory.getId());

        return categoryMapper.toDetailResponse(updatedCategory);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CacheNames.CATEGORY_BY_ID, key = "#id"),
            @CacheEvict(value = RedisConfig.CacheNames.CATEGORY_BY_SLUG, allEntries = true),
            @CacheEvict(value = RedisConfig.CacheNames.CATEGORY_TREE, allEntries = true)
    })
    public void deleteCategory(Long id) {
        log.info("Deleting category with ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        Long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new InvalidRequestException("Cannot delete category with " + productCount + " products. Move or delete products first.");
        }

        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            throw new InvalidRequestException("Cannot delete category with child categories. Delete or reassign children first.");
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully with ID: {}", id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CacheNames.CATEGORY_BY_ID, key = "#id")
    public CategoryDetailResponse getCategoryById(Long id) {
        log.info("Fetching category with ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        return categoryMapper.toDetailResponse(category);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CacheNames.CATEGORY_BY_SLUG, key = "#slug")
    public CategoryDetailResponse getCategoryBySlug(String slug) {
        log.info("Fetching category with slug: {}", slug);

        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));

        return categoryMapper.toDetailResponse(category);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CacheNames.CATEGORY_TREE, key = "'full-tree'")
    public List<CategoryTreeResponse> getCategoryTree() {
        log.info("Fetching full category tree");

        List<Category> rootCategories = categoryRepository.findByParentIsNull();

        return rootCategories.stream()
                .map(this::buildCategoryTree)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> getRootCategories() {
        log.info("Fetching root categories");

        List<Category> rootCategories = categoryRepository.findByParentIsNull();

        return categoryMapper.toSummaryResponseList(rootCategories);
    }

    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> getChildCategories(Long parentId) {
        log.info("Fetching child categories for parent ID: {}", parentId);

        if (!categoryRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Category", parentId);
        }

        List<Category> children = categoryRepository.findByParentId(parentId);

        return categoryMapper.toSummaryResponseList(children);
    }

    private CategoryTreeResponse buildCategoryTree(Category category) {
        List<CategoryTreeResponse> children = null;

        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            children = category.getChildren().stream()
                    .map(this::buildCategoryTree)
                    .toList();
        }

        Long productCount = productRepository.countByCategoryId(category.getId());

        return new CategoryTreeResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getImageUrl(),
                productCount,
                children
        );
    }

}
