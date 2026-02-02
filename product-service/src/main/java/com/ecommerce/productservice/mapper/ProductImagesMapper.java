package com.ecommerce.productservice.mapper;

import com.ecommerce.productservice.dto.request.ProductImageRequest;
import com.ecommerce.productservice.dto.response.ProductImageResponse;
import com.ecommerce.productservice.entity.ProductImages;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductImagesMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    ProductImages toEntity(ProductImageRequest request);

    ProductImageResponse toResponse(ProductImages productImages);

    List<ProductImageResponse> toResponseList(List<ProductImages> productImages);

}
