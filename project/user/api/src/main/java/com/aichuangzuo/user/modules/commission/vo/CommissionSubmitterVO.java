package com.aichuangzuo.user.modules.commission.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 约稿投稿人摘要 VO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSubmitterVO {

    private Long submitterId;

    private String nickname;

    private String avatarUrl;
}
