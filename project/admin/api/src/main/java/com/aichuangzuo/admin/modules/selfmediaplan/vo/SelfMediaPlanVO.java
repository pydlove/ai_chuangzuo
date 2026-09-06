package com.aichuangzuo.admin.modules.selfmediaplan.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SelfMediaPlanVO {

    private Long id;
    private Long userId;

    /** 用户昵称。 */
    private String userNickname;
    /** 用户手机号。 */
    private String userPhone;
    /** 用户邮箱。 */
    private String userEmail;

    private String platformKey;
    private String platformName;
    private String nicheName;
    private String personaName;

    /** 内容支柱（名称 + 占比）。 */
    private List<Pillar> contentPillars;

    /** 是否 AI 推荐：1 是，0 否。 */
    private Integer isRecommendedByAi;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class Pillar {
        private String name;
        private Integer percent;
    }
}
