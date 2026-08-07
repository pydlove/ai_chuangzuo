package com.aichuangzuo.admin.modules.earnings.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class WithdrawQueryRequest {

    private Long userId;

    private String bizNo;

    private Integer status;

    @Min(1)
    private int page = 1;

    @Min(1)
    private int size = 20;
}
