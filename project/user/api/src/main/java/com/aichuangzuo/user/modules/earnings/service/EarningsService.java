package com.aichuangzuo.user.modules.earnings.service;

import com.aichuangzuo.user.modules.earnings.dto.request.ListEarningsRequest;
import com.aichuangzuo.user.modules.earnings.vo.AccountSummaryVO;
import com.aichuangzuo.user.modules.earnings.vo.EarningsRecordPageVO;
import com.aichuangzuo.user.modules.earnings.vo.MonthlySettlementVO;
import com.aichuangzuo.user.modules.earnings.vo.SettleLastMonthResultVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户收益服务。
 */
public interface EarningsService {

    /**
     * 查询当前用户账户汇总。
     *
     * @param userId 用户ID
     * @return 汇总视图
     */
    AccountSummaryVO getSummary(Long userId);

    /**
     * 查询当前用户按月聚合的结算列表。
     *
     * @param userId 用户ID
     * @return 月度结算列表
     */
    List<MonthlySettlementVO> getMonthlySettlementList(Long userId);

    /**
     * 分页查询当前用户收益记录。
     *
     * @param userId  用户ID
     * @param request 查询条件
     * @return 分页视图
     */
    EarningsRecordPageVO listEarnings(Long userId, ListEarningsRequest request);

    /**
     * 结算上月的未结算收益。
     *
     * @param userId 用户ID
     * @return 结算结果
     */
    SettleLastMonthResultVO settleLastMonth(Long userId);

    /**
     * 记录一条收益（内部使用，供风格市场、邀请奖励等调用）。
     *
     * @param userId        用户ID
     * @param type          收益类型
     * @param sourceType    来源类型
     * @param sourceId      来源业务ID
     * @param title         标题
     * @param description   描述
     * @param amount        金额（必须为正）
     * @param settlementMonth 归属月份 YYYY-MM
     */
    void recordEarnings(Long userId, String type, String sourceType, String sourceId,
                        String title, String description, BigDecimal amount, String settlementMonth);
}
