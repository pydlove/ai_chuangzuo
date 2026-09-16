package com.aichuangzuo.admin.modules.simulation.dto;

import lombok.Data;

/**
 * 模拟生成文章-随机抽取的真实用户行。
 */
@Data
public class SimulationUserPickRow {

    private Long id;
    private String nickname;
    private String email;
}
