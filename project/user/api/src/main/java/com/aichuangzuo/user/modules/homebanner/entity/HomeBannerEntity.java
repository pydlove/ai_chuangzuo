package com.aichuangzuo.user.modules.homebanner.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("a_home_banner")
public class HomeBannerEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String imageUrl;

    private String linkUrl;

    private Integer sort;

    @TableLogic
    @TableField(select = false)
    private Integer isDeleted;
}
