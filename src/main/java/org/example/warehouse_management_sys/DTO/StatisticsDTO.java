package org.example.warehouse_management_sys.DTO;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StatisticsDTO {
    private String materialId;
    private String materialName;
    private String categoryName;
    private String supplierName;  // 新增：供应商名称
    private String yearMonth;
    private BigDecimal inQuantity;
    private BigDecimal outQuantity;
    private BigDecimal netChange;
    private Integer recordCount;
    private BigDecimal avgQuantity;  // 平均出入库数量
}
