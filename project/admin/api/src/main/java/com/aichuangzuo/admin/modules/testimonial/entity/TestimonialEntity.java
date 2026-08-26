package com.aichuangzuo.admin.modules.testimonial.entity;

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

    /** 头像 URL */
    private String avatarUrl;

    /** 评价人姓名 */
    private String name;

    /** 评价人身份/职位 */
    private String title;

    /** 星级：1-5 */
    private Integer starRating;

    /** 评价内容 */
    private String reviewText;

    /** 排序权重，小在前 */
    private Integer sort;

    /** 是否启用：0-禁用，1-启用 */
    private Integer isEnabled;
}
