package org.example.warehouse_management_sys;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("org.example.warehouse_management_sys.Mapper")
public class WarehouseManagementSysApplication {
    public static void main(String[] args) {
        SpringApplication.run(WarehouseManagementSysApplication.class, args);
        System.out.println("========================================");
        System.out.println("工业物料库存管理系统启动成功！");
        System.out.println("访问地址: http://localhost:63342/warehouse_management_sys/industrial-warehouse/static/index.html?_ijt=60i1ujoir7u5qnbvgr90n6keh5&_ij_reload=RELOAD_ON_SAVE");
        System.out.println("========================================");
    }
}