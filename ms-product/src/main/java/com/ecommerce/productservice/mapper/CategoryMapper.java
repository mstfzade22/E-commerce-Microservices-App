package com.ecommerce.productservice.mapper;

import com.ecommerce.productservice.dto.request.CreateCategoryRequest;
import com.ecommerce.productservice.dto.request.UpdateCategoryRequest;
import com.ecommerce.productservice.dto.response.CategoryCreateResponse;
import com.ecommerce.productservice.dto.response.CategoryDetailResponse;
import com.ecommerce.productservice.dto.response.CategorySummaryResponse;
import com.ecommerce.productservice.dto.response.CategoryTreeResponse;
import com.ecommerce.productservice.entity.Category;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.hibernate.Hibernate;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    @Mapping(target = "productCount", ignore = true)
    @Mapping(target = "parent", source = "parent", qualifiedByName = "toParentSummary")
    @Mapping(target = "children", source = "children", qualifiedByName = "toChildrenSummary")
    CategoryDetailResponse toDetailResponse(Category category);

    CategorySummaryResponse toSummaryResponse(Category category);

    @Mapping(target = "productCount", ignore = true)
    @Mapping(target = "children", source = "children", qualifiedByName = "toChildrenTree")
    CategoryTreeResponse toTreeResponse(Category category);

    @Mapping(target = "message", constant = "Category created successfully")
    CategoryCreateResponse toCreateResponse(Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateCategoryRequest request, @MappingTarget Category category);

    List<CategorySummaryResponse> toSummaryResponseList(List<Category> categories);

    List<CategoryTreeResponse> toTreeResponseList(List<Category> categories);

    @Named("toParentSummary")
    default CategorySummaryResponse toParentSummary(Category parent) {
        if (parent == null || !Hibernate.isInitialized(parent)) {
            return null;
        }
        return toSummaryResponse(parent);
    }

    @Named("toChildrenSummary")
    default List<CategorySummaryResponse> toChildrenSummary(List<Category> children) {
        if (children == null || !Hibernate.isInitialized(children)) {
            return new ArrayList<>();
        }
        return toSummaryResponseList(children);
    }

    @Named("toChildrenTree")
    default List<CategoryTreeResponse> toChildrenTree(List<Category> children) {
        if (children == null || !Hibernate.isInitialized(children)) {
            return new ArrayList<>();
        }
        return toTreeResponseList(children);
    }

}
