package com.aichuangzuo.admin.modules.user.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除用户请求。
 */
@Data
public class BatchDeleteUserRequest {

    /** 待删除用户 ID 列表。 */
    @NotEmpty(message = "请至少选择一个用户")
    private List<Long> ids;
}
