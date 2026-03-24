package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.request.CreateCategoryRequest;
import com.ecommerce.productservice.dto.request.UpdateCategoryRequest;
import com.ecommerce.productservice.dto.response.CategoryCreateResponse;
import com.ecommerce.productservice.dto.response.CategoryDetailResponse;
import com.ecommerce.productservice.dto.response.CategorySummaryResponse;
import com.ecommerce.productservice.dto.response.CategoryTreeResponse;
import com.ecommerce.productservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Category Management", description = "APIs for managing product categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get root categories", description = "Retrieves all top-level categories")
    public ResponseEntity<List<CategorySummaryResponse>> getRootCategories() {
        log.info("Request to get root categories");

        List<CategorySummaryResponse> categories = categoryService.getRootCategories();

        return ResponseEntity.ok(categories);
    }

    @GetMapping("/tree")
    @Operation(summary = "Get category tree", description = "Retrieves full category hierarchy as a tree")
    public ResponseEntity<List<CategoryTreeResponse>> getCategoryTree() {
        log.info("Request to get category tree");

        List<CategoryTreeResponse> tree = categoryService.getCategoryTree();

        return ResponseEntity.ok(tree);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieves a category by its ID")
    public ResponseEntity<CategoryDetailResponse> getCategoryById(@PathVariable Long id) {
        log.info("Request to get category with ID: {}", id);

        CategoryDetailResponse category = categoryService.getCategoryById(id);

        return ResponseEntity.ok(category);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug", description = "Retrieves a category by its slug")
    public ResponseEntity<CategoryDetailResponse> getCategoryBySlug(@PathVariable String slug) {
        log.info("Request to get category with slug: {}", slug);

        CategoryDetailResponse category = categoryService.getCategoryBySlug(slug);

        return ResponseEntity.ok(category);
    }

    @GetMapping("/{parentId}/children")
    @Operation(summary = "Get child categories", description = "Retrieves direct child categories of a parent")
    public ResponseEntity<List<CategorySummaryResponse>> getChildCategories(@PathVariable Long parentId) {
        log.info("Request to get child categories for parent ID: {}", parentId);

        List<CategorySummaryResponse> children = categoryService.getChildCategories(parentId);

        return ResponseEntity.ok(children);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category", description = "Creates a new category (Admin only)")
    public ResponseEntity<CategoryCreateResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        log.info("Request to create category with slug: {}", request.slug());

        CategoryCreateResponse response = categoryService.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update category", description = "Updates an existing category (Admin only)")
    public ResponseEntity<CategoryDetailResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        log.info("Request to update category with ID: {}", id);

        CategoryDetailResponse response = categoryService.updateCategory(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete category", description = "Deletes a category (Admin only)")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        log.info("Request to delete category with ID: {}", id);

        categoryService.deleteCategory(id);

        return ResponseEntity.noContent().build();
    }

}
