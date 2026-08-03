package com.aichuangzuo.admin.modules.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserImportRowErrorVO {

    private Integer rowIndex;
    private String email;
    private List<String> errors;
}
