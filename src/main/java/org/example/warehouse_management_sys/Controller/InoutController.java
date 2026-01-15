package org.example.warehouse_management_sys.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.InoutDTO;
import org.example.warehouse_management_sys.DTO.TraceQueryDTO;
import org.example.warehouse_management_sys.Entity.InoutRecord;
import org.example.warehouse_management_sys.Service.InoutService;
import org.example.warehouse_management_sys.Utils.Result;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/inout")
public class InoutController {

    @Resource
    private InoutService inoutService;

    @PostMapping("/operate")
    public Result<Map<String, String>> materialInout(@Valid @RequestBody InoutDTO dto) {
        String recordId = inoutService.materialInout(dto);

        Map<String, String> result = new HashMap<>();
        result.put("recordId", recordId);
        result.put("message", "出入库操作成功");

        return Result.success(result);
    }

    @PostMapping("/trace")
    public Result<List<InoutRecord>> traceMaterial(@Valid @RequestBody TraceQueryDTO dto) {
        List<InoutRecord> records = inoutService.traceMaterial(
                dto.getMaterialId(),
                dto.getStartDate(),
                dto.getEndDate()
        );
        return Result.success(records);
    }

    @GetMapping("/recent")
    public Result<List<InoutRecord>> getRecentRecords(
            @RequestParam(defaultValue = "50") Integer limit) {
        List<InoutRecord> records = inoutService.getRecentRecords(limit);
        return Result.success(records);
    }

    @GetMapping("/material/{materialId}")
    public Result<List<InoutRecord>> getRecordsByMaterialId(@PathVariable String materialId) {
        List<InoutRecord> records = inoutService.getRecordsByMaterialId(materialId);
        return Result.success(records);
    }
}
