package org.example.warehouse_management_sys.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.StatisticsDTO;
import org.example.warehouse_management_sys.Entity.Material;
import org.example.warehouse_management_sys.Mapper.StatisticsMapper;
import org.example.warehouse_management_sys.Mapper.MaterialMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StatisticsService {

    @Resource
    private StatisticsMapper statisticsMapper;

    @Resource
    private MaterialMapper materialMapper;

//    @Resource
//    private SupplierMapper supplierMapper;

    public List<StatisticsDTO> getMonthlyStatistics(Integer year, Integer month) {
        return statisticsMapper.monthlyStatistics(year, month);
    }

    public List<StatisticsDTO> getCategoryFlowStatistics(String startDate, String endDate) {
        return statisticsMapper.categoryFlowStatistics(startDate, endDate);
    }

    public List<Map<String, Object>> getMaterialTrend(String materialId, Integer months) {
        if (months == null || months <= 0) {
            months = 6;
        }
        return statisticsMapper.materialTrend(materialId, months);
    }

    public Map<String, Object> getStockOverview() {
        return statisticsMapper.stockOverview();
    }
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取所有物料 - 使用注入的mapper实例
            List<Material> materials = materialMapper.selectAllWithStatus();

            // 计算物料总数
            int totalMaterials = materials != null ? materials.size() : 0;
            result.put("total_materials", totalMaterials);

            // 计算库存总值
            BigDecimal totalValue = BigDecimal.ZERO;
            if (materials != null) {
                for (Material material : materials) {
                    if (material.getCurrentStock() != null && material.getUnitPrice() != null) {
                        try {
                            BigDecimal value = material.getCurrentStock().multiply(material.getUnitPrice());
                            totalValue = totalValue.add(value);
                        } catch (Exception e) {
                            log.warn("计算物料价值出错: materialId={}, currentStock={}, unitPrice={}",
                                    material.getMaterialId(), material.getCurrentStock(), material.getUnitPrice());
                        }
                    }
                }
            }
            result.put("total_value", totalValue.setScale(2, RoundingMode.HALF_UP));

            // 获取低库存物料数量
            List<Material> lowStockMaterials = materialMapper.selectLowStock();
            result.put("low_stock_count", lowStockMaterials != null ? lowStockMaterials.size() : 0);

            // 获取高库存物料数量
            List<Material> highStockMaterials = materialMapper.selectHighStock();
            result.put("high_stock_count", highStockMaterials != null ? highStockMaterials.size() : 0);

            // 今日出入库数量（暂时返回0，需要出入库记录表）
            result.put("today_inout", 0);

            log.info("获取概览数据: 物料总数={}, 库存总值={}", totalMaterials, totalValue);

        } catch (Exception e) {
            log.error("获取概览数据失败", e);
            // 返回默认值
            result.put("total_materials", 0);
            result.put("total_value", BigDecimal.ZERO);
            result.put("low_stock_count", 0);
            result.put("high_stock_count", 0);
            result.put("today_inout", 0);
        }

        return result;
    }



    public Map<String, Object> getAlertStatistics() {
        return statisticsMapper.alertStatistics();
    }

    public List<Map<String, Object>> getSupplierStatistics() {
        return statisticsMapper.supplierMaterialStatistics();
    }
}
