package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserAccountPageVO {
    private List<UserAccountVO> list;
    private long total;
}
