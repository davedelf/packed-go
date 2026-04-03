package com.packed_go.analytics_service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Performance de una categoría
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPerformanceDTO {
    
    private Long categoryId;
    private String categoryName;
    private Long itemsSold;
    private BigDecimal revenue;
}
