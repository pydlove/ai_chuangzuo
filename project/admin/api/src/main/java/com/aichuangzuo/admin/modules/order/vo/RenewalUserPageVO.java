package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.util.List;

@Data
public class RenewalUserPageVO {
    private List<RenewalUserVO> list;
    private Long total;
}
