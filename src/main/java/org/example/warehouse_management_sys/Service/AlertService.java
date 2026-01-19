package org.example.warehouse_management_sys.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.Entity.StockAlert;
import org.example.warehouse_management_sys.Mapper.StockAlertMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class AlertService {

    @Resource
    private StockAlertMapper stockAlertMapper;
    public List<StockAlert> getUnhandledAlerts(String alertType) {
        return stockAlertMapper.selectUnhandled(alertType);
    }

    public List<StockAlert> getAllAlerts() {
        return stockAlertMapper.selectAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean handleAlert(Integer alertId, String handleRemark) {
        int result = stockAlertMapper.handleAlert(alertId, handleRemark);
        log.info("处理预警: alertId={}, remark={}", alertId, handleRemark);
        return result > 0;
    }

    public List<StockAlert> getAlertsByMaterialId(String materialId) {
        return stockAlertMapper.selectByMaterialId(materialId);
    }
}
