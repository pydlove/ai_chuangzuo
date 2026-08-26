package com.aichuangzuo.admin.modules.testimonial.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestimonialVO {

    private Long id;
    private String avatarUrl;
    private String name;
    private String title;
    private Integer starRating;
    private String reviewText;
    private Integer sort;
    private Integer isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
