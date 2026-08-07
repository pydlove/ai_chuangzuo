package com.aichuangzuo.user.modules.earnings.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端处理提现申请请求。
 */
@Data
public class WithdrawProcessRequest {

    /** 状态：2-已通过，3-已拒绝。 */
    @NotNull(message = "处理状态不能为空")
    private Integer status;

    /** 处理结果备注（拒绝时必填）。 */
    private String remark;
}
