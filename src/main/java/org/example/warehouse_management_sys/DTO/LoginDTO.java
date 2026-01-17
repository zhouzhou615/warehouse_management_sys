package org.example.warehouse_management_sys.DTO;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class LoginDTO {
    @NotBlank(message = "用户名/ID不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}