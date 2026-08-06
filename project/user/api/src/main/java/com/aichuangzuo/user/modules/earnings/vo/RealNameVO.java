package com.aichuangzuo.user.modules.earnings.vo;

import lombok.Data;

/**
 * 用户实名信息视图。
 */
@Data
public class RealNameVO {

    private String realName;

    private String idCard;

    private Boolean verified;
}
