package com.aichuangzuo.admin.modules.earnings.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddCoinRequest {

    /**
     * 增加创作币数量，必须大于 0，最多 2 位小数。
     */
    @NotNull(message = "创作币数量不能为空")
    @DecimalMin(value = "0.01", message = "创作币数量必须大于 0")
    private BigDecimal amount;

    /**
     * 备注，可选。
     */
    private String remark;
}
