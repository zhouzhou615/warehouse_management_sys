package org.example.warehouse_management_sys.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.SupplierDTO;
import org.example.warehouse_management_sys.Entity.Supplier;
import org.example.warehouse_management_sys.Service.SupplierService;
import org.example.warehouse_management_sys.Utils.Result;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/supplier")
public class SupplierController {

    @Resource
    private SupplierService supplierService;

    @PostMapping("/add")
    public Result<Boolean> addSupplier(@Valid @RequestBody SupplierDTO dto) {
        boolean success = supplierService.addSupplier(dto);
        return Result.success("供应商新增成功", success);
    }
    @PutMapping("/update")
    public Result<Boolean> updateSupplier(@Valid @RequestBody SupplierDTO dto) {
        boolean success = supplierService.updateSupplier(dto);
        return Result.success("供应商更新成功", success);
    }

    @DeleteMapping("/delete/{supplierId}")
    public Result<Boolean> deleteSupplier(@PathVariable String supplierId) {
        boolean success = supplierService.deleteSupplier(supplierId);
        return Result.success("供应商删除成功", success);
    }

    @GetMapping("/get/{supplierId}")
    public Result<Supplier> getSupplierById(@PathVariable String supplierId) {
        Supplier supplier = supplierService.getSupplierById(supplierId);
        if (supplier == null) {
            return Result.error("供应商不存在");
        }
        return Result.success(supplier);
    }

    @GetMapping("/list")
    public Result<List<Supplier>> getAllSuppliers() {
        List<Supplier> list = supplierService.getAllSuppliers();
        return Result.success(list);
    }

    @GetMapping("/active")
    public Result<List<Supplier>> getActiveSuppliers() {
        List<Supplier> list = supplierService.getActiveSuppliers();
        return Result.success(list);
    }

    @GetMapping("/search")
    public Result<List<Supplier>> searchSuppliers(@RequestParam(required = false) String keyword) {
        List<Supplier> list = supplierService.searchSuppliers(keyword);
        return Result.success(list);
    }

    @PutMapping("/updateStatus")
    public Result<Boolean> updateSupplierStatus(@RequestParam String supplierId,
                                                @RequestParam String status) {
        boolean success = supplierService.updateSupplierStatus(supplierId, status);
        return Result.success("供应商状态更新成功", success);
    }
}