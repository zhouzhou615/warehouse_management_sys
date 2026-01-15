package org.example.warehouse_management_sys.Mapper;

import org.apache.ibatis.annotations.Param;
import org.example.warehouse_management_sys.Entity.StockAlert;

import java.util.List;

public interface StockAlertMapper {

    // 插入预警记录
    int insert(StockAlert alert);

    // 查询未处理的预警
    List<StockAlert> selectUnhandled(@Param("alertType") String alertType);

    // 查询所有预警
    List<StockAlert> selectAll();

    // 处理预警
    int handleAlert(@Param("alertId") Integer alertId,
                    @Param("handleRemark") String handleRemark);

    // 根据物料ID查询预警
    List<StockAlert> selectByMaterialId(@Param("materialId") String materialId);
}
