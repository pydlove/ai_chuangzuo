package com.aichuangzuo.admin.modules.earnings.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AccountQueryRequest {
    private Long userId;
    private String nickname;
    private String phone;
    private String email;

    @Min(1)
    private int page = 1;

    @Min(1)
    private int size = 20;
}
