package com.aichuangzuo.user.modules.membership.vo;

import lombok.Data;

/**
 * 当前会员状态响应。
 */
@Data
public class MembershipStatusVO {

    /** 是否拥有有效会员。 */
    private boolean hasMembership;

    /** 等级 key：basic / pro / flagship。 */
    private String level;

    /** 等级显示名。 */
    private String levelName;

    /** 到期日期 yyyy-MM-dd。 */
    private String expiresAt;
}
