package org.example.warehouse_management_sys.DTO;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AnomalyDetectionDTO {
    private String recordId;
    private String materialId;
    private String materialName;
    private BigDecimal quantity;
    private String operatorId;
    private String operatorName;
    private LocalDateTime operationTime;
    private String remark;
    private BigDecimal beforeStock;
    private BigDecimal afterStock;
    private Integer cluster; // K-means聚类结果
    private String anomalyReason; // 异常原因
    private BigDecimal zScore; // Z分数
    private BigDecimal avgQuantity; // 历史平均出库量
    private BigDecimal stdQuantity; // 历史标准差
    private Integer recordCount; // 历史记录数
}