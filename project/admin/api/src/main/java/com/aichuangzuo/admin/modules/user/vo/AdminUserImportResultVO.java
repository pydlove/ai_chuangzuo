package com.aichuangzuo.admin.modules.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserImportResultVO {

    private boolean success;
    private int totalRows;
    private int importedCount;
    private List<AdminUserImportRowErrorVO> errors;
}
