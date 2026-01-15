package org.example.warehouse_management_sys.DTO;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
public class TraceQueryDTO {

    @NotBlank(message = "物料编号不能为空")
    private String materialId;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
