package com.aichuangzuo.user.modules.lottery.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryDisplayWinnerVO {

    private Long id;
    private String nickname;
    private String avatarUrl;
    private String prizeName;
    private Integer prizeLevel;
    private LocalDateTime winTime;
    private Integer isReal;

    /** 会员等级：basic/pro/flagship；非会员或已过期为 null。 */
    private String memberLevel;
}
