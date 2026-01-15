package org.example.warehouse_management_sys.Entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Material {
    private String materialId;
    private String materialName;
    private String specification;
    private Integer categoryId;
    private String categoryName; // 关联查询字段
    private String unit;
    private BigDecimal safeStockMin;
    private BigDecimal safeStockMax;
    private BigDecimal currentStock;
    private BigDecimal unitPrice;
    private String supplierId;        // 更新：改为供应商ID
    private String supplierName;      // 关联查询字段
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String stockStatus; // 库存状态（视图字段）
}