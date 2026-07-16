package com.aichuangzuo.admin.modules.topictitle.vo;

import com.aichuangzuo.shared.entity.TopicTitle;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标题管理列表行 VO。
 */
@Data
public class TopicTitleAdminVO {

    private Long id;
    private String title;
    private String summary;
    private String direction;
    private Integer useCount;
    private LocalDateTime createdAt;

    public static TopicTitleAdminVO from(TopicTitle entity) {
        TopicTitleAdminVO vo = new TopicTitleAdminVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setSummary(entity.getSummary());
        vo.setDirection(entity.getDirection());
        vo.setUseCount(entity.getUseCount());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
