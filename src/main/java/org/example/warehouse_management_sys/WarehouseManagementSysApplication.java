package org.example.warehouse_management_sys;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@MapperScan("org.example.warehouse_management_sys.Mapper")
@EnableScheduling  // 确保启用定时任务
public class WarehouseManagementSysApplication {
    public static void main(String[] args) {
        SpringApplication.run(WarehouseManagementSysApplication.class, args);
        System.out.println("========================================");
        System.out.println("工业物料库存管理系统启动成功！");
        System.out.println("访问地址: http://localhost:8080/warehouse/index.html");
        System.out.println("========================================");
    }
}