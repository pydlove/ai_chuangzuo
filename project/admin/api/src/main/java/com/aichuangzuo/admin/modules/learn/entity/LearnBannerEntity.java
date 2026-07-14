package com.aichuangzuo.admin.modules.learn.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_learn_banner")
public class LearnBannerEntity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 图片 URL */
    private String imageUrl;

    /** 点击跳转链接 */
    private String linkUrl;

    /** 排序权重，小在前 */
    private Integer sort;
}
