package com.aichuangzuo.admin.modules.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserPageVO {
    private List<AdminUserVO> list;
    private long total;
}
