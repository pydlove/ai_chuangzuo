package com.aichuangzuo.admin.modules.user.vo;

import lombok.Data;

/**
 * 用户下拉选项 VO（用于发布者选择等场景）。
 */
@Data
public class AdminUserOptionVO {

    private Long id;
    private String nickname;
    private String email;
}
