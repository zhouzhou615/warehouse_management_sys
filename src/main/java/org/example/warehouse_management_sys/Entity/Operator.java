package org.example.warehouse_management_sys.Entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Operator {
    private String operatorId;
    private String operatorName;
    private String department;
    private String phone;
    private String email;
    private String status;
    private LocalDateTime createTime;
    private String password;  // 新增密码字段
}