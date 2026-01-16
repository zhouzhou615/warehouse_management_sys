package org.example.warehouse_management_sys.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.SupplierDTO;
import org.example.warehouse_management_sys.Entity.Supplier;
import org.example.warehouse_management_sys.Mapper.SupplierMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SupplierService {

    @Resource
    private SupplierMapper supplierMapper;

    /**
     * 新增供应商
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean addSupplier(SupplierDTO dto) {
        // 检查供应商编号是否已存在
        Supplier existing = supplierMapper.selectById(dto.getSupplierId());
        if (existing != null) {
            throw new IllegalArgumentException("供应商编号已存在");
        }

        // 转换并插入
        Supplier supplier = new Supplier();
        BeanUtils.copyProperties(dto, supplier);
        supplier.setCreateTime(LocalDateTime.now());
        supplier.setUpdateTime(LocalDateTime.now());

        int result = supplierMapper.insert(supplier);
        log.info("新增供应商: {}, 名称: {}, 结果: {}",
                dto.getSupplierId(), dto.getSupplierName(), result > 0);
        return result > 0;
    }

    /**
     * 更新供应商信息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSupplier(SupplierDTO dto) {
        // 检查供应商是否存在
        Supplier existing = supplierMapper.selectById(dto.getSupplierId());
        if (existing == null) {
            throw new IllegalArgumentException("供应商不存在");
        }

        Supplier supplier = new Supplier();
        BeanUtils.copyProperties(dto, supplier);
        supplier.setUpdateTime(LocalDateTime.now());

        int result = supplierMapper.update(supplier);
        log.info("更新供应商: {}, 结果: {}", dto.getSupplierId(), result > 0);
        return result > 0;
    }

    /**
     * 删除供应商
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSupplier(String supplierId) {
        int result = supplierMapper.delete(supplierId);
        if (result == 0) {
            throw new IllegalArgumentException("供应商不存在");
        }
        log.info("删除供应商: {}", supplierId);
        return true;
    }

    /**
     * 根据ID查询供应商
     */
    public Supplier getSupplierById(String supplierId) {
        return supplierMapper.selectById(supplierId);
    }

    /**
     * 查询所有供应商
     */
    public List<Supplier> getAllSuppliers() {
        return supplierMapper.selectAll();
    }

    /**
     * 查询合作中的供应商
     */
    public List<Supplier> getActiveSuppliers() {
        return supplierMapper.selectActive();
    }

    /**
     * 模糊查询供应商
     */
    public List<Supplier> searchSuppliers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSuppliers();
        }
        return supplierMapper.selectByKeyword(keyword.trim());
    }

    /**
     * 更新供应商状态
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSupplierStatus(String supplierId, String status) {
        // 验证状态值
        if (!"合作中".equals(status) && !"暂停".equals(status) && !"终止".equals(status)) {
            throw new IllegalArgumentException("状态值无效");
        }

        int result = supplierMapper.updateStatus(supplierId, status);
        if (result == 0) {
            throw new IllegalArgumentException("供应商不存在");
        }
        log.info("更新供应商状态: {}, 新状态: {}", supplierId, status);
        return true;
    }
}