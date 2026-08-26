package com.aichuangzuo.user.modules.membership.vo;

import lombok.Data;

import java.util.List;

/**
 * 我的订单分页视图。
 */
@Data
public class OrderPageVO {

    private List<OrderVO> list;

    private long total;

    private long page;

    private long pageSize;
}
