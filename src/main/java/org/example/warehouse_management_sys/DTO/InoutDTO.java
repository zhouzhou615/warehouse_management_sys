package org.example.warehouse_management_sys.DTO;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class InoutDTO {

    @NotBlank(message = "物料编号不能为空")
    private String materialId;

    @NotBlank(message = "出入库类型不能为空")
    @Pattern(regexp = "入库|出库", message = "出入库类型只能是'入库'或'出库'")
    private String inoutType;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.01", message = "数量必须大于0")
    private BigDecimal quantity;

    @NotBlank(message = "操作员编号不能为空")
    private String operatorId;

    private String remark;
}

