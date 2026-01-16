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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public void debugTodayInOut() {
        try {
            log.info("=== 开始调试今日出入库数据 ===");

            // 1. 获取当前日期和时间
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = LocalDate.now();
            log.info("当前系统时间: {}", now);
            log.info("当前系统日期: {}", today);

            // 2. 直接查询今日出入库记录
            String testSql = "SELECT COUNT(*) as count, SUM(quantity) as total " +
                    "FROM inout_record " +
                    "WHERE operation_time >= CURRENT_DATE";
            log.info("执行的SQL: {}", testSql);

            // 3. 通过 mapper 查询今日数据
            Map<String, Object> todayStats = statisticsMapper.getTodayInOutStatistics();
            log.info("今日出入库统计结果: {}", todayStats);

            // 4. 查询库存总览
            Map<String, Object> overview = statisticsMapper.stockOverview();
            log.info("库存总览结果: {}", overview);

            // 5. 直接查询inout_record表统计
            String directCountSql = "SELECT COUNT(*) FROM inout_record WHERE operation_time >= CURRENT_DATE";
            log.info("直接查询今日记录数SQL: {}", directCountSql);

            log.info("=== 调试结束 ===");

        } catch (Exception e) {
            log.error("调试过程中发生错误", e);
        }
    }

    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("开始获取概览数据...");

            // 先调试今日数据
            this.debugTodayInOut();

            // 从数据库获取概览数据
            Map<String, Object> overview = statisticsMapper.stockOverview();
            log.info("stockOverview查询结果: {}", overview);

            if (overview != null) {
                result.putAll(overview);

                // 如果today_inout为0，尝试其他方法获取
                Object todayInOut = overview.get("today_inout");
                if (todayInOut == null ||
                        (todayInOut instanceof Number && ((Number) todayInOut).doubleValue() == 0)) {

                    log.warn("stockOverview返回的today_inout为0或null，尝试通过getTodayInOutStatistics获取");

                    Map<String, Object> todayStats = statisticsMapper.getTodayInOutStatistics();
                    log.info("getTodayInOutStatistics查询结果: {}", todayStats);

                    if (todayStats != null) {
                        Object todayIn = todayStats.get("today_in");
                        Object todayOut = todayStats.get("today_out");

                        BigDecimal total = BigDecimal.ZERO;
                        if (todayIn != null) {
                            total = total.add(new BigDecimal(todayIn.toString()));
                        }
                        if (todayOut != null) {
                            total = total.add(new BigDecimal(todayOut.toString()));
                        }

                        result.put("today_inout", total);
                        log.info("通过getTodayInOutStatistics计算的今日出入库总量: {}", total);
                    } else {
                        result.put("today_inout", 0);
                    }
                } else {
                    log.info("使用stockOverview的today_inout值: {}", todayInOut);
                }
            } else {
                log.warn("stockOverview返回null");
            }

            // 确保所有必要字段都存在
            result.putIfAbsent("total_materials", 0);
            result.putIfAbsent("total_value", BigDecimal.ZERO);
            result.putIfAbsent("low_stock_count", 0);
            result.putIfAbsent("high_stock_count", 0);
            result.putIfAbsent("today_inout", 0);

            log.info("最终返回的概览数据: {}", result);

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
