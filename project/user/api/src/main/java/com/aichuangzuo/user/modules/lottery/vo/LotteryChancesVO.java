package com.aichuangzuo.user.modules.lottery.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LotteryChancesVO {

    private int availableChances;
    private boolean freeChanceAvailable;
}
