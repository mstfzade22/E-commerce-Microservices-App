package com.ecommerce.inventoryservice.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductUpdatedEvent(Long id, Integer stock) {}
