package com.aichuangzuo.admin.modules.earnings.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AccountQueryRequest {
    private Long userId;
    private String nickname;
    private String phone;
    private String email;
    /**
     * 用户类型：0-机器人，1-真实用户；不传表示全部。
     */
    private Integer userType;
    private String sortBy;

    @Min(1)
    private int page = 1;

    @Min(1)
    private int size = 20;
}
