package org.example.warehouse_management_sys.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.warehouse_management_sys.DTO.StatisticsDTO;

import java.util.List;
import java.util.Map;
@Mapper
public interface StatisticsMapper {

    // 月度出入库统计
    List<StatisticsDTO> monthlyStatistics(@Param("year") Integer year,
                                          @Param("month") Integer month);

    // 按类别统计流量
    List<StatisticsDTO> categoryFlowStatistics(@Param("startDate") String startDate,
                                               @Param("endDate") String endDate);

    // 物料出入库趋势
    List<Map<String, Object>> materialTrend(@Param("materialId") String materialId,
                                            @Param("months") Integer months);

    // 库存总览
    Map<String, Object> stockOverview();

    // 预警统计
    Map<String, Object> alertStatistics();

    // 供应商物料统计
    List<Map<String, Object>> supplierMaterialStatistics();
}
