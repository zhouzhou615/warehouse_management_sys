package org.example.warehouse_management_sys.DTO;
import lombok.Data;
import javax.validation.constraints.*;

@Data
public class SupplierDTO {

    @NotBlank(message = "供应商编号不能为空")
    @Size(max = 20, message = "供应商编号长度不能超过20")
    private String supplierId;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100, message = "供应商名称长度不能超过100")
    private String supplierName;

    private String contactPerson;
    private String phone;
    private String address;
    private String qualificationLevel;
    private String status;
}
