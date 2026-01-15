package org.example.warehouse_management_sys.DTO;
// MonthlyQueryDTO.java - 月度查询参数
import lombok.Data;
import javax.validation.constraints.*;

@Data
public class MonthlyQueryDTO {

    @NotNull(message = "年份不能为空")
    @Min(value = 2000, message = "年份不能小于2000")
    @Max(value = 2100, message = "年份不能大于2100")
    private Integer year;

    @NotNull(message = "月份不能为空")
    @Min(value = 1, message = "月份必须在1-12之间")
    @Max(value = 12, message = "月份必须在1-12之间")
    private Integer month;
}

