package org.example.warehouse_management_sys.Entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class DB4AIEntity {

    // 时序预测结果
    @Data
    public static class TimeSeriesPrediction {
        private String materialId;
        private String materialName;
        private LocalDateTime predictionDate;
        private BigDecimal predictedStock;
        private BigDecimal safeStockMin;
        private BigDecimal safeStockMax;
        private Boolean needPurchase; // 是否需要采购
        private BigDecimal purchaseQuantity; // 建议采购量
    }

    // 异常检测结果
    @Data
    public static class AnomalyDetection {
        private String recordId;
        private String materialId;
        private String materialName;
        private String inoutType;
        private BigDecimal quantity;
        private LocalDateTime operationTime;
        private Double anomalyScore; // 异常分数
        private String anomalyReason; // 异常原因
        private Map<String, Object> historicalStats; // 历史统计数据
    }

    // 采购建议清单
    @Data
    public static class PurchaseRecommendation {
        private String materialId;
        private String materialName;
        private String supplierId;
        private String supplierName;
        private BigDecimal currentStock;
        private BigDecimal safeStockMin;
        private BigDecimal predictedStock; // 预测库存
        private BigDecimal recommendedQuantity; // 建议采购量
        private Integer urgencyLevel; // 紧急程度 1-5
        private String recommendationReason;
    }
}