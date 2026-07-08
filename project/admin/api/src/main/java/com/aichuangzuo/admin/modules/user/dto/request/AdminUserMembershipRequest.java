package com.aichuangzuo.admin.modules.user.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 管理端修改用户会员到期时间。
 * expireDate 为 null 表示清空（非会员）；否则取"到期日"，后端会自动转成"次日 00:00"存储。
 */
@Data
public class AdminUserMembershipRequest {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;
}