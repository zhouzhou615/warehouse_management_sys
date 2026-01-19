package org.example.warehouse_management_sys.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.InoutDTO;
import org.example.warehouse_management_sys.Entity.InoutRecord;
import org.example.warehouse_management_sys.Entity.Material;
import org.example.warehouse_management_sys.Mapper.InoutRecordMapper;
import org.example.warehouse_management_sys.Mapper.MaterialMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class InoutService {

    @Resource
    private InoutRecordMapper inoutRecordMapper;

    @Resource
    private MaterialMapper materialMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate; // 添加JdbcTemp

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 出入库操作
     * 说明：第200-208行生成唯一单据号，格式为"出入库类型+时间戳+随机数"
     * 第210-215行查询物料当前库存并验证是否存在
     * 第217-224行根据出入库类型验证库存是否充足并计算操作后库存
     * 第226-234行构建出入库记录对象，记录操作前后库存快照
     * 第236-244行在同一事务中插入记录（触发器自动更新物料表库存）
     */
    @Transactional(rollbackFor = Exception.class)
    public String materialInout(InoutDTO dto) {
        // 查询物料当前库存
        Material material = materialMapper.selectById(dto.getMaterialId());
        if (material == null) {
            throw new IllegalArgumentException("物料不存在");
        }
        // 验证库存（出库时需要）
        if ("出库".equals(dto.getInoutType()) &&
                material.getCurrentStock().compareTo(dto.getQuantity()) < 0) {
            throw new IllegalArgumentException(
                    String.format("库存不足! 当前库存: %s, 出库数量: %s",
                            material.getCurrentStock(), dto.getQuantity()));
        }
        // 生成单据号（与存储过程生成方式一致）
        String recordId = dto.getInoutType() + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        // 调用存储过程
        Map<String, Object> params = new HashMap<>();
        params.put("p_material_id", dto.getMaterialId());
        params.put("p_inout_type", dto.getInoutType());
        params.put("p_quantity", dto.getQuantity());
        params.put("p_operator_id", dto.getOperatorId());
        params.put("p_remark", dto.getRemark());
        params.put("p_result", null);  // 输出参数
        params.put("p_record_id", null); // 输出参数
        // 使用 MyBatis 调用存储过程
        inoutRecordMapper.callMaterialInoutProcedure(params);
        String result = (String) params.get("p_result");
        String generatedRecordId = (String) params.get("p_record_id");

        if (!"成功".equals(result)) {
            throw new RuntimeException("出入库操作失败");
        }

        log.info("出入库操作成功: 单据号={}, 物料={}, 类型={}, 数量={}",
                generatedRecordId, dto.getMaterialId(), dto.getInoutType(), dto.getQuantity());

        return generatedRecordId;
    }

    /**
     * 物料追溯查询
     */
    public List<InoutRecord> traceMaterial(String materialId,
                                           LocalDateTime startDate,
                                           LocalDateTime endDate) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(3);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        return inoutRecordMapper.selectByDateRange(materialId, startDate, endDate);
    }

    /**
     * 查询最近操作记录
     */
    public List<InoutRecord> getRecentRecords(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 50;
        }
        return inoutRecordMapper.selectRecent(limit);
    }

    /**
     * 根据物料ID查询所有记录
     */
    public List<InoutRecord> getRecordsByMaterialId(String materialId) {
        return inoutRecordMapper.selectByMaterialId(materialId);
    }
    /**
     * 获取带有异常标记的出入库记录
     */
    public List<Map<String, Object>> getInoutRecordsWithAnomaly(String materialId,
                                                                LocalDateTime startDate,
                                                                LocalDateTime endDate) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT ir.*, m.MATERIAL_NAME, o.OPERATOR_NAME, " +
                            "va.anomaly_reason, va.z_score, va.cluster " +
                            "FROM INOUT_RECORD ir " +
                            "LEFT JOIN MATERIAL m ON ir.MATERIAL_ID = m.MATERIAL_ID " +
                            "LEFT JOIN OPERATOR o ON ir.OPERATOR_ID = o.OPERATOR_ID " +
                            "LEFT JOIN V_ANOMALY_INOUT_LOOSE_4517 va ON ir.RECORD_ID = va.RECORD_ID " +
                            "WHERE 1=1 "
            );

            List<Object> params = new ArrayList<>();

            if (materialId != null && !materialId.trim().isEmpty()) {
                sql.append(" AND ir.MATERIAL_ID = ? ");
                params.add(materialId);
            }

            if (startDate != null) {
                sql.append(" AND ir.OPERATION_TIME >= ? ");
                params.add(startDate);
            }

            if (endDate != null) {
                sql.append(" AND ir.OPERATION_TIME <= ? ");
                params.add(endDate);
            }

            sql.append(" ORDER BY ir.OPERATION_TIME DESC");

            return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
                Map<String, Object> record = new HashMap<>();
                record.put("recordId", rs.getString("RECORD_ID"));
                record.put("materialId", rs.getString("MATERIAL_ID"));
                record.put("materialName", rs.getString("MATERIAL_NAME"));
                record.put("inoutType", rs.getString("INOUT_TYPE"));
                record.put("quantity", rs.getBigDecimal("QUANTITY"));
                record.put("operatorId", rs.getString("OPERATOR_ID"));
                record.put("operatorName", rs.getString("OPERATOR_NAME"));
                record.put("operationTime", rs.getTimestamp("OPERATION_TIME"));
                record.put("remark", rs.getString("REMARK"));
                record.put("beforeStock", rs.getBigDecimal("BEFORE_STOCK"));
                record.put("afterStock", rs.getBigDecimal("AFTER_STOCK"));

                // 异常标记
                String anomalyReason = rs.getString("anomaly_reason");
                BigDecimal zScore = rs.getBigDecimal("z_score");

                record.put("hasAnomaly", anomalyReason != null && anomalyReason.contains("超出历史均值"));
                record.put("anomalyReason", anomalyReason);
                record.put("zScore", zScore);
                record.put("cluster", rs.getInt("CLUSTER"));

                // 根据Z分数设置风险等级
                if (zScore != null) {
                    if (zScore.compareTo(new BigDecimal("3.0")) >= 0) {
                        record.put("riskLevel", "高风险");
                        record.put("riskColor", "#f56c6c");
                    } else if (zScore.compareTo(new BigDecimal("2.0")) >= 0) {
                        record.put("riskLevel", "中风险");
                        record.put("riskColor", "#e6a23c");
                    } else {
                        record.put("riskLevel", "正常");
                        record.put("riskColor", "#67c23a");
                    }
                } else {
                    record.put("riskLevel", "正常");
                    record.put("riskColor", "#67c23a");
                }

                return record;
            });
        } catch (Exception e) {
            log.error("获取带异常标记的记录失败", e);
            return Collections.emptyList();
        }
    }
}
