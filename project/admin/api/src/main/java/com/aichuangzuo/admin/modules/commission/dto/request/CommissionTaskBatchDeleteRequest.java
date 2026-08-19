package com.aichuangzuo.admin.modules.commission.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 约稿任务批量删除请求。
 */
@Data
public class CommissionTaskBatchDeleteRequest {

    @NotEmpty(message = "请选择要删除的任务")
    private List<Long> ids;
}
