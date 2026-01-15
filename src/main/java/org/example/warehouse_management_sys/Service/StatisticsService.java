package org.example.warehouse_management_sys.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.StatisticsDTO;
import org.example.warehouse_management_sys.Mapper.StatisticsMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StatisticsService {

    @Resource
    private StatisticsMapper statisticsMapper;

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

    public Map<String, Object> getAlertStatistics() {
        return statisticsMapper.alertStatistics();
    }

    public List<Map<String, Object>> getSupplierStatistics() {
        return statisticsMapper.supplierMaterialStatistics();
    }
}
