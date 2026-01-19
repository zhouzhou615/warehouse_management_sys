
package org.example.warehouse_management_sys.DTO;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DB4AIPredictDTO {
    private String materialId;
    private String materialName;
    private BigDecimal currentStock;
    private BigDecimal safeStockMin;
    private BigDecimal predictedStock;
    private BigDecimal predictedChange;
    private Integer dayNum; // 预测的天数序号
    private String predictionSource; // 预测来源：AI预测、当前库存、趋势预测
    private String unit; // 物料单位
}
