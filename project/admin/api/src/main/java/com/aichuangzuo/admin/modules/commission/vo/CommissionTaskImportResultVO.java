package com.aichuangzuo.admin.modules.commission.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionTaskImportResultVO {
    private boolean success;
    private int totalRows;
    private int importedCount;
    private List<CommissionTaskImportRowErrorVO> errors;
}
