package com.aichuangzuo.user.modules.testimonial.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("a_testimonial")
public class TestimonialEntity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String avatarUrl;

    private String name;

    private String title;

    private Integer starRating;

    private String reviewText;

    private Integer sort;

    private Integer isEnabled;
}
