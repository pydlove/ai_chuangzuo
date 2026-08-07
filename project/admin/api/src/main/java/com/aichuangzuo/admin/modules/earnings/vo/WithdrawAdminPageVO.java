package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.util.List;

@Data
public class WithdrawAdminPageVO {

    private List<WithdrawAdminVO> list;

    private long total;
}
