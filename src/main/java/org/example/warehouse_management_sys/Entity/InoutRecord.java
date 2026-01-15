package org.example.warehouse_management_sys.Entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InoutRecord {
    private String recordId;
    private String materialId;
    private String materialName; // 关联查询字段
    private String inoutType;
    private BigDecimal quantity;
    private String operatorId;
    private String operatorName; // 关联查询字段
    private LocalDateTime operationTime;
    private String remark;
    private BigDecimal beforeStock;
    private BigDecimal afterStock;
}