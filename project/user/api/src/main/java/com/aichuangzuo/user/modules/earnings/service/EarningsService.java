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

    /**
     * 记录一条已结算收益（直接到账，无需再次结算）。
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
    void recordSettledEarnings(Long userId, String type, String sourceType, String sourceId,
                               String title, String description, BigDecimal amount, String settlementMonth);

    /**
     * 记录邀请奖励收益（已结算）。
     *
     * @param userId          邀请人用户ID
     * @param inviteeId       被邀请人用户ID
     * @param planKey         套餐 key
     * @param planName        套餐显示名
     * @param cycle           周期 month/quarter/year
     * @param orderAmount     被邀请人订单金额
     * @param firstPurchase   是否首购
     * @param commissionRate  返佣比例
     * @param commissionAmount 返佣金额（创作币）
     * @param settlementMonth 归属月份 YYYY-MM
     */
    void recordInviteRewardEarnings(Long userId, Long inviteeId, String planKey, String planName,
                                    String cycle, BigDecimal orderAmount, boolean firstPurchase,
                                    BigDecimal commissionRate, BigDecimal commissionAmount,
                                    String settlementMonth);
}
