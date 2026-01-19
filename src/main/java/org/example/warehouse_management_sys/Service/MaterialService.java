package org.example.warehouse_management_sys.Service;


import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.MaterialDTO;
import org.example.warehouse_management_sys.Entity.Material;
import org.example.warehouse_management_sys.Entity.MaterialCategory;
import org.example.warehouse_management_sys.Mapper.MaterialMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class MaterialService {

    @Resource
    private MaterialMapper materialMapper;

    /**
     * 新增物料
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean addMaterial(MaterialDTO dto) {
        // 校验数据
        if (dto.getSafeStockMax().compareTo(dto.getSafeStockMin()) < 0) {
            throw new IllegalArgumentException("安全库存上限必须大于等于下限");
        }
        // 检查物料编号是否已存在
        Material existing = materialMapper.selectById(dto.getMaterialId());
        if (existing != null) {
            throw new IllegalArgumentException("物料编号已存在");
        }
        // 转换并插入
        Material material = new Material();
        BeanUtils.copyProperties(dto, material);
        int result = materialMapper.insert(material);
        log.info("新增物料: {}, 供应商: {}, 结果: {}",
                dto.getMaterialId(), dto.getSupplierId(), result > 0);
        return result > 0;
    }

    /**
     * 更新物料信息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMaterial(MaterialDTO dto) {
        Material material = new Material();
        BeanUtils.copyProperties(dto, material);

        int result = materialMapper.update(material);
        log.info("更新物料: {}, 结果: {}", dto.getMaterialId(), result > 0);
        return result > 0;
    }

    /**
     * 更新安全库存
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSafeStock(String materialId, BigDecimal min, BigDecimal max) {
        if (max.compareTo(min) < 0) {
            throw new IllegalArgumentException("安全库存上限必须大于等于下限");
        }

        int result = materialMapper.updateSafeStock(materialId, min, max);
        log.info("更新安全库存: {}, min={}, max={}, 结果: {}",
                materialId, min, max, result > 0);
        return result > 0;
    }

    /**
     * 删除物料(仅当库存为0)
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMaterial(String materialId) {
        int result = materialMapper.delete(materialId);
        if (result == 0) {
            throw new IllegalArgumentException("删除失败,可能库存不为0或物料不存在");
        }
        log.info("删除物料: {}", materialId);
        return true;
    }

    /**
     * 根据ID查询物料
     */
    public Material getMaterialById(String materialId) {
        return materialMapper.selectById(materialId);
    }

    /**
     * 查询所有物料
     */
    public List<Material> getAllMaterials() {
        return materialMapper.selectAllWithStatus();
    }

    /**
     * 模糊查询物料
     */
    public List<Material> searchMaterials(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllMaterials();
        }
        return materialMapper.selectByKeyword(keyword.trim());
    }

    /**
     * 查询低库存物料
     */
    public List<Material> getLowStockMaterials() {
        return materialMapper.selectLowStock();
    }

    /**
     * 查询高库存物料
     */
    public List<Material> getHighStockMaterials() {
        return materialMapper.selectHighStock();
    }

    /**
     * 获取所有类别
     */
    public List<MaterialCategory> getAllCategories() {
        return materialMapper.selectAllCategories();
    }

    /**
     * 根据类别查询物料
     */
    public List<Material> getMaterialsByCategory(Integer categoryId) {
        return materialMapper.selectByCategoryId(categoryId);
    }

    /**
     * 根据供应商查询物料
     */
    public List<Material> getMaterialsBySupplier(String supplierId) {
        return materialMapper.selectBySupplierId(supplierId);
    }
}

