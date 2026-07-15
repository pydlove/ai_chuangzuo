package com.aichuangzuo.admin.modules.homebanner.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HomeBannerVO {
    private Long id;
    private String imageUrl;
    private String linkUrl;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
