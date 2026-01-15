package org.example.warehouse_management_sys.Mapper;

import org.apache.ibatis.annotations.Param;
import org.example.warehouse_management_sys.Entity.Material;
import org.example.warehouse_management_sys.Entity.MaterialCategory;

import java.math.BigDecimal;
import java.util.List;

public interface MaterialMapper {

    // 新增物料
    int insert(Material material);

    // 更新物料信息
    int update(Material material);

    // 更新安全库存
    int updateSafeStock(@Param("materialId") String materialId,
                        @Param("safeStockMin") BigDecimal safeStockMin,
                        @Param("safeStockMax") BigDecimal safeStockMax);

    // 删除物料（仅当库存为0）
    int delete(@Param("materialId") String materialId);

    // 根据ID查询物料
    Material selectById(@Param("materialId") String materialId);

    // 查询所有物料（含库存状态和供应商信息）
    List<Material> selectAllWithStatus();

    // 模糊查询物料
    List<Material> selectByKeyword(@Param("keyword") String keyword);

    // 查询低库存物料
    List<Material> selectLowStock();

    // 查询高库存物料
    List<Material> selectHighStock();

    // 查询所有物料类别
    List<MaterialCategory> selectAllCategories();

    // 根据类别查询物料
    List<Material> selectByCategoryId(@Param("categoryId") Integer categoryId);

    // 根据供应商查询物料
    List<Material> selectBySupplierId(@Param("supplierId") String supplierId);
}

