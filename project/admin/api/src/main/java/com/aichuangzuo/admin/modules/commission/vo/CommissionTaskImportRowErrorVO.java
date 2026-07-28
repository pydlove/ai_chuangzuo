package com.aichuangzuo.admin.modules.commission.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionTaskImportRowErrorVO {
    private Integer rowIndex;
    private String title;
    private List<String> errors;
}
