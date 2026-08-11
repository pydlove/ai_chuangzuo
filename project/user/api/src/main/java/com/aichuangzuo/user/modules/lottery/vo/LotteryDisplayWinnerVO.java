package com.aichuangzuo.user.modules.lottery.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryDisplayWinnerVO {

    private Long id;
    private String nickname;
    private String avatarUrl;
    private String prizeName;
    private LocalDateTime winTime;
    private Integer isReal;
}
