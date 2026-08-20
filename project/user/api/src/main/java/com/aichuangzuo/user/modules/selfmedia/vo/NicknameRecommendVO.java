package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

import java.util.List;

/**
 * 平台账号昵称与简介推荐结果。
 */
@Data
public class NicknameRecommendVO {

    /** 推荐方案列表（通常 3 个），每个包含昵称和简介。 */
    private List<Option> options;

    @Data
    public static class Option {
        /** 推荐昵称。 */
        private String nickname;

        /** 推荐账号简介。 */
        private String bio;
    }
}
