package com.aichuangzuo.user.modules.learn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_learn_banner")
public class LearnBannerEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String imageUrl;

    private String linkUrl;

    private Integer sort;

    @TableLogic
    @TableField(select = false)
    private Integer isDeleted;
}
