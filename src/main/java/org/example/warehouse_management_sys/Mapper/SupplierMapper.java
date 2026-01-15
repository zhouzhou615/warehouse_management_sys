package org.example.warehouse_management_sys.Mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.warehouse_management_sys.Entity.Supplier;

import java.util.List;
@Mapper
public interface SupplierMapper {

    // 新增供应商
    int insert(Supplier supplier);

    // 更新供应商信息
    int update(Supplier supplier);

    // 删除供应商
    int delete(@Param("supplierId") String supplierId);

    // 根据ID查询供应商
    Supplier selectById(@Param("supplierId") String supplierId);

    // 查询所有供应商
    List<Supplier> selectAll();

    // 查询合作中的供应商
    List<Supplier> selectActive();

    // 模糊查询供应商
    List<Supplier> selectByKeyword(@Param("keyword") String keyword);

    // 更新供应商状态
    int updateStatus(@Param("supplierId") String supplierId,
                     @Param("status") String status);
}

