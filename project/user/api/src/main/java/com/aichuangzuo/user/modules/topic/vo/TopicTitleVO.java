package com.aichuangzuo.user.modules.topic.vo;

import com.aichuangzuo.shared.entity.TopicTitle;
import lombok.Data;

/**
 * 用户端随机标题 VO。
 */
@Data
public class TopicTitleVO {

    private Long id;
    private String title;
    private String summary;

    public static TopicTitleVO from(TopicTitle entity) {
        TopicTitleVO vo = new TopicTitleVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setSummary(entity.getSummary());
        return vo;
    }
}
