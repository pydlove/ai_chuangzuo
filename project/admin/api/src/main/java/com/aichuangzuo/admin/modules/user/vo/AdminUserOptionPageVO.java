package com.aichuangzuo.admin.modules.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserOptionPageVO {
    private List<AdminUserOptionVO> list;
    private long total;
}
