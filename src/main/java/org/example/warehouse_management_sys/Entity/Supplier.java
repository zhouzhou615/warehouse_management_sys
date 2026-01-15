package org.example.warehouse_management_sys.Entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Supplier {
    private String supplierId;
    private String supplierName;
    private String contactPerson;
    private String phone;
    private String address;
    private String qualificationLevel;
    private String status; // 合作中、暂停、终止
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}