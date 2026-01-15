package org.example.warehouse_management_sys.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.MaterialDTO;
import org.example.warehouse_management_sys.Entity.Material;
import org.example.warehouse_management_sys.Entity.MaterialCategory;
import org.example.warehouse_management_sys.Service.MaterialService;
import org.example.warehouse_management_sys.Utils.Result;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/material")
public class MaterialController {

    @Resource
    private MaterialService materialService;

    @PostMapping("/add")
    public Result<Boolean> addMaterial(@Valid @RequestBody MaterialDTO dto) {
        boolean success = materialService.addMaterial(dto);
        return Result.success("物料新增成功", success);
    }

    @PutMapping("/update")
    public Result<Boolean> updateMaterial(@Valid @RequestBody MaterialDTO dto) {
        boolean success = materialService.updateMaterial(dto);
        return Result.success("物料更新成功", success);
    }

    @PutMapping("/updateSafeStock")
    public Result<Boolean> updateSafeStock(@RequestParam String materialId,
                                           @RequestParam BigDecimal min,
                                           @RequestParam BigDecimal max) {
        boolean success = materialService.updateSafeStock(materialId, min, max);
        return Result.success("安全库存更新成功", success);
    }

    @DeleteMapping("/delete/{materialId}")
    public Result<Boolean> deleteMaterial(@PathVariable String materialId) {
        boolean success = materialService.deleteMaterial(materialId);
        return Result.success("物料删除成功", success);
    }

    @GetMapping("/get/{materialId}")
    public Result<Material> getMaterialById(@PathVariable String materialId) {
        Material material = materialService.getMaterialById(materialId);
        if (material == null) {
            return Result.error("物料不存在");
        }
        return Result.success(material);
    }

    @GetMapping("/list")
    public Result<List<Material>> getAllMaterials() {
        List<Material> list = materialService.getAllMaterials();
        return Result.success(list);
    }

    @GetMapping("/search")
    public Result<List<Material>> searchMaterials(@RequestParam(required = false) String keyword) {
        List<Material> list = materialService.searchMaterials(keyword);
        return Result.success(list);
    }

    @GetMapping("/lowStock")
    public Result<List<Material>> getLowStockMaterials() {
        List<Material> list = materialService.getLowStockMaterials();
        return Result.success(list);
    }

    @GetMapping("/highStock")
    public Result<List<Material>> getHighStockMaterials() {
        List<Material> list = materialService.getHighStockMaterials();
        return Result.success(list);
    }

    @GetMapping("/categories")
    public Result<List<MaterialCategory>> getAllCategories() {
        List<MaterialCategory> list = materialService.getAllCategories();
        return Result.success(list);
    }

    @GetMapping("/category/{categoryId}")
    public Result<List<Material>> getMaterialsByCategory(@PathVariable Integer categoryId) {
        List<Material> list = materialService.getMaterialsByCategory(categoryId);
        return Result.success(list);
    }

    @GetMapping("/supplier/{supplierId}")
    public Result<List<Material>> getMaterialsBySupplier(@PathVariable String supplierId) {
        List<Material> list = materialService.getMaterialsBySupplier(supplierId);
        return Result.success(list);
    }
}

