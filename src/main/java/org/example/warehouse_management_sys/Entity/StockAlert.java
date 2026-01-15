package org.example.warehouse_management_sys.Entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockAlert {
    private Integer alertId;
    private String materialId;
    private String materialName; // 关联查询字段
    private String alertType;
    private BigDecimal currentStock;
    private BigDecimal safeThreshold;
    private LocalDateTime alertTime;
    private String status;
    private LocalDateTime handleTime;
    private String handleRemark;
    private String alertDesc; // 预警描述（计算字段）
}