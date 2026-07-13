package com.aichuangzuo.user.modules.learn.vo;

import lombok.Data;

/**
 * 创作学院 - 文章上一篇/下一篇引用 VO。
 * <p>用于文章详情底部的阅读链导航。</p>
 */
@Data
public class LearnArticleRefVO {
    private Long id;
    private String title;
    private String categoryName;
}
