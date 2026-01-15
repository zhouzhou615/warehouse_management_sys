package org.example.warehouse_management_sys.Entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MaterialCategory {
    private Integer categoryId;
    private String categoryName;
    private String categoryDesc;
    private Integer parentId;
    private LocalDateTime createTime;
}
