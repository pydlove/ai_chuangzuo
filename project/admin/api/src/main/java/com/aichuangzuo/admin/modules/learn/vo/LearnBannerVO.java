package com.aichuangzuo.admin.modules.learn.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearnBannerVO {
    private Long id;
    private String imageUrl;
    private String linkUrl;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
