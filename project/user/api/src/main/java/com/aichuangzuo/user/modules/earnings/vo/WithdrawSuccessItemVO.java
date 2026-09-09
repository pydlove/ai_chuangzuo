package com.aichuangzuo.user.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现成功动态视图（工作台实时提现滚动条）。
 */
@Data
public class WithdrawSuccessItemVO {

    /** 脱敏后的用户昵称。 */
    private String nickname;

    /** 提现创作币数量。 */
    private BigDecimal amount;

    /** 提现成功时间。 */
    private LocalDateTime processedAt;
}
