package com.aichuangzuo.user.modules.earnings.vo;

import lombok.Data;

import java.util.List;

/**
 * 收益记录分页视图。
 */
@Data
public class EarningsRecordPageVO {

    private List<EarningsRecordVO> list;

    private long total;

    private long page;

    private long pageSize;
}
