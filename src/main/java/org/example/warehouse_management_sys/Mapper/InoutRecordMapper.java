package org.example.warehouse_management_sys.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.warehouse_management_sys.Entity.InoutRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Mapper
public interface InoutRecordMapper {

    // 插入出入库记录
    int insert(InoutRecord record);

    // 查询物料的出入库历史
    List<InoutRecord> selectByMaterialId(@Param("materialId") String materialId);

    // 时间范围查询
    List<InoutRecord> selectByDateRange(@Param("materialId") String materialId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // 查询最近N条记录
    List<InoutRecord> selectRecent(@Param("limit") Integer limit);

    // 按物料统计出入库
    List<Map<String, Object>> statisticsByMaterial(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);
}
