package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营方案库条目（匿名，不含任何用户信息）。
 */
@Data
public class SelfMediaPlanGalleryVO {

    private Long id;
    private String platformKey;
    private String platformName;
    private String nicheName;
    private String personaName;
    private List<PillarVO> pillars;
    private Integer isRecommendedByAi;
    private LocalDateTime updatedAt;
}
