package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

import java.util.List;

@Data
public class NicknameCheckVO {

    /** 是否与自媒体定位契合。 */
    private Boolean fit;

    /** 判定理由。 */
    private String reason;

    /** 不契合时给出的 3 个建议昵称（含简介）。 */
    private List<Suggestion> suggestions;

    @Data
    public static class Suggestion {
        /** 建议昵称。 */
        private String nickname;

        /** 对应账号简介。 */
        private String bio;
    }
}
