package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

/**
 * 平台账号昵称与简介推荐结果。
 */
@Data
public class NicknameRecommendVO {

    /** 推荐昵称。 */
    private String nickname;

    /** 推荐账号简介。 */
    private String bio;
}
