package com.aichuangzuo.admin.modules.testimonial.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserReviewVO {

    private Long id;

    private Long userId;

    private String nickname;

    private Integer starRating;

    private String content;

    private Integer status;

    private Integer isShowOnHomepage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
