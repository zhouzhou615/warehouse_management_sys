//// [file name]: DB4AIConfig.java
//package org.example.warehouse_management_sys.Config;
//
//import lombok.extern.slf4j.Slf4j;
//import org.example.warehouse_management_sys.Service.DB4AIService;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.EnableScheduling;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.Resource;
//
//@Slf4j
//@Configuration
//@EnableScheduling
//public class DB4AIConfig {
//
//    @Resource
//    private DB4AIService db4aiService;
//
//    @PostConstruct
//    public void initDB4AI() {
//        log.info("=== 开始初始化DB4AI功能 ===");
//
//        if (db4aiService.checkDB4AIAvailability()) {
//            log.info("DB4AI功能可用，开始初始化...");
//
//            try {
//                // 1. 填充训练数据
//                db4aiService.populateTrainingData();
//                log.info("训练数据填充完成");
//
//                // 2. 创建异常检测模型
//                db4aiService.createAnomalyDetectionModel();
//                log.info("异常检测模型创建完成");
//
//                // 3. 批量创建低库存物料的预测模型
//                int created = db4aiService.batchCreateForecastModels();
//                log.info("批量创建了{}个预测模型", created);
//
//                // 4. 记录今日库存快照
//                db4aiService.recordDailyStock();
//                log.info("今日库存快照记录完成");
//
//                log.info("DB4AI初始化完成，功能已就绪");
//            } catch (Exception e) {
//                log.error("DB4AI初始化失败", e);
//                log.warn("DB4AI部分功能可能无法正常使用");
//            }
//        } else {
//            log.warn("DB4AI功能不可用，请检查数据库配置");
//            log.warn("1. 确保postgresql.conf中已设置:");
//            log.warn("   enable_dynamic_workload = on");
//            log.warn("   enable_dump_prediction = on");
//            log.warn("2. 确保数据库已重启");
//            log.warn("3. 确保DB4AI组件已正确安装");
//            log.warn("DB4AI相关功能将自动降级到传统统计方法");
//        }
//
//        log.info("=== DB4AI初始化结束 ===");
//    }
//}