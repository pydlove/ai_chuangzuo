package com.aichuangzuo.user.modules.lottery.vo;

import lombok.Data;

import java.util.List;

@Data
public class LotteryDisplayWinnerPageVO {

    private List<LotteryDisplayWinnerVO> list;
    private long total;
    private long page;
    private long pageSize;
}
