package com.aichuangzuo.user.modules.testimonial.vo;

import lombok.Data;

@Data
public class TestimonialVO {

    private Long id;
    private String avatarUrl;
    private String name;
    private String title;
    private Integer starRating;
    private String reviewText;
}
