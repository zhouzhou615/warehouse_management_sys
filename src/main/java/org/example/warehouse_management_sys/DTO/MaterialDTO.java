package org.example.warehouse_management_sys.DTO;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class MaterialDTO {

    @NotBlank(message = "物料编号不能为空")
    @Size(max = 20, message = "物料编号长度不能超过20")
    private String materialId;

    @NotBlank(message = "物料名称不能为空")
    @Size(max = 100, message = "物料名称长度不能超过100")
    private String materialName;

    private String specification;

    @NotNull(message = "物料类别不能为空")
    private Integer categoryId;

    @NotBlank(message = "计量单位不能为空")
    private String unit;

    @NotNull(message = "安全库存下限不能为空")
    @DecimalMin(value = "0", message = "安全库存下限不能为负")
    private BigDecimal safeStockMin;

    @NotNull(message = "安全库存上限不能为空")
    @DecimalMin(value = "0", message = "安全库存上限不能为负")
    private BigDecimal safeStockMax;

    private BigDecimal currentStock;
    private BigDecimal unitPrice;
    private String supplierId;  // 更新：改为供应商ID
}