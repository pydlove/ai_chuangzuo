package com.aichuangzuo.admin.modules.user.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class UserImportExcelRowData {

    @ExcelProperty("邮箱（选填，与手机号二选一）")
    private String email;

    @ExcelProperty("手机号（选填，与邮箱二选一）")
    private String phone;

    @ExcelProperty("昵称（必填）")
    private String nickname;

    @ExcelProperty("密码（选填，留空默认 Aichuangzuo@123）")
    private String password;

    @ExcelProperty("用户类型（选填，0=机器人，1=真实用户；默认 0）")
    private String userType;
}
