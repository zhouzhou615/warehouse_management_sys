package org.example.warehouse_management_sys.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.InoutDTO;
import org.example.warehouse_management_sys.Entity.InoutRecord;
import org.example.warehouse_management_sys.Entity.Material;
import org.example.warehouse_management_sys.Mapper.InoutRecordMapper;
import org.example.warehouse_management_sys.Mapper.MaterialMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class InoutService {

    @Resource
    private InoutRecordMapper inoutRecordMapper;

    @Resource
    private MaterialMapper materialMapper;

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
        // 生成单据号
        String recordId = dto.getInoutType() +
                LocalDateTime.now().format(FORMATTER) +
                String.format("%03d", new Random().nextInt(1000));

        // 查询物料当前库存
        Material material = materialMapper.selectById(dto.getMaterialId());
        if (material == null) {
            throw new IllegalArgumentException("物料不存在");
        }

        BigDecimal beforeStock = material.getCurrentStock();
        BigDecimal afterStock;

        // 验证并计算库存
        if ("出库".equals(dto.getInoutType())) {
            if (beforeStock.compareTo(dto.getQuantity()) < 0) {
                throw new IllegalArgumentException(
                        String.format("库存不足! 当前库存: %s, 出库数量: %s",
                                beforeStock, dto.getQuantity()));
            }
            afterStock = beforeStock.subtract(dto.getQuantity());
        } else {
            afterStock = beforeStock.add(dto.getQuantity());
        }

        // 构建记录
        InoutRecord record = new InoutRecord();
        record.setRecordId(recordId);
        record.setMaterialId(dto.getMaterialId());
        record.setInoutType(dto.getInoutType());
        record.setQuantity(dto.getQuantity());
        record.setOperatorId(dto.getOperatorId());
        record.setRemark(dto.getRemark());
        record.setBeforeStock(beforeStock);
        record.setAfterStock(afterStock);

        // 插入记录（触发器会自动更新库存）
        int result = inoutRecordMapper.insert(record);

        log.info("出入库操作: 单据号={}, 物料={}, 类型={}, 数量={}, 操作前库存={}, 操作后库存={}",
                recordId, dto.getMaterialId(), dto.getInoutType(),
                dto.getQuantity(), beforeStock, afterStock);

        if (result == 0) {
            throw new RuntimeException("出入库操作失败");
        }

        return recordId;
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
}
