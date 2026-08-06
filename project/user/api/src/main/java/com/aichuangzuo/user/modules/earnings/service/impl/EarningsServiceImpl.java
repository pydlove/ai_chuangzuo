package com.aichuangzuo.user.modules.earnings.service.impl;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.earnings.dto.request.ListEarningsRequest;
import com.aichuangzuo.user.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.user.modules.earnings.enums.EarningsType;
import com.aichuangzuo.user.modules.earnings.mapper.EarningsRecordMapper;
import com.aichuangzuo.user.modules.earnings.service.EarningsService;
import com.aichuangzuo.user.modules.earnings.vo.AccountSummaryVO;
import com.aichuangzuo.user.modules.earnings.vo.EarningsRecordPageVO;
import com.aichuangzuo.user.modules.earnings.vo.EarningsRecordVO;
import com.aichuangzuo.user.modules.earnings.vo.MonthlySettlementVO;
import com.aichuangzuo.user.modules.message.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户收益服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EarningsServiceImpl implements EarningsService {

    private final EarningsRecordMapper earningsRecordMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    @Override
    public AccountSummaryVO getSummary(Long userId) {
        LambdaQueryWrapper<EarningsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EarningsRecord::getUserId, userId)
                .eq(EarningsRecord::getIsDeleted, 0);

        List<EarningsRecord> records = earningsRecordMapper.selectList(wrapper);
        BigDecimal total = records.stream()
                .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AccountSummaryVO vo = new AccountSummaryVO();
        vo.setCoinBalance(getCoinBalance(userId));
        vo.setTotalEarnings(total);
        return vo;
    }

    @Override
    public List<MonthlySettlementVO> getMonthlySummary(Long userId) {
        return earningsRecordMapper.selectMonthlySettlementList(userId);
    }

    @Override
    public String nextBizNo() {
        return "ER" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    @Override
    public EarningsRecordPageVO listEarnings(Long userId, ListEarningsRequest request) {
        LambdaQueryWrapper<EarningsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EarningsRecord::getUserId, userId)
                .eq(EarningsRecord::getIsDeleted, 0);

        if (StringUtils.hasText(request.getMonth())) {
            wrapper.eq(EarningsRecord::getSettlementMonth, request.getMonth());
        }

        wrapper.orderByDesc(EarningsRecord::getCreatedAt);

        IPage<EarningsRecord> page = new Page<>(request.getPage(), request.getPageSize());
        page = earningsRecordMapper.selectPage(page, wrapper);

        EarningsRecordPageVO vo = new EarningsRecordPageVO();
        vo.setList(page.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        vo.setTotal(page.getTotal());
        vo.setPage(page.getCurrent());
        vo.setPageSize(page.getSize());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordEarnings(Long userId, String type, String sourceType, String sourceId,
                               String title, String description, BigDecimal amount, String settlementMonth) {
        doRecordEarnings(userId, type, sourceType, sourceId, title, description, amount, settlementMonth);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordInviteRewardEarnings(Long userId, Long inviteeId, String planKey, String planName,
                                           String cycle, BigDecimal orderAmount, boolean firstPurchase,
                                           BigDecimal commissionRate, BigDecimal commissionAmount,
                                           String settlementMonth) {
        if (commissionAmount == null || commissionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("收益金额必须大于 0");
        }
        if (!StringUtils.hasText(settlementMonth)) {
            throw new IllegalArgumentException("归属月份不能为空");
        }

        User invitee = userMapper.selectById(inviteeId);
        String inviteeName = invitee == null ? "好友" : (StringUtils.hasText(invitee.getNickname()) ? invitee.getNickname() : "好友");
        String purchaseType = firstPurchase ? "首次购买" : "续费";
        String title = "邀请奖励";
        String description = String.format("%s %s %s（%s），返佣 %s 创作币",
                inviteeName, purchaseType, planName, cycleLabel(cycle), commissionAmount.toPlainString());

        EarningsRecord record = new EarningsRecord();
        record.setUserId(userId);
        record.setType(EarningsType.INVITE_REWARD.getCode());
        record.setSourceType("invite");
        record.setSourceId(inviteeId.toString());
        record.setPlanKey(planKey);
        record.setPlanName(planName);
        record.setCycle(cycle);
        record.setOrderAmount(orderAmount);
        record.setCommissionRate(commissionRate);
        record.setIsFirstPurchase(firstPurchase ? 1 : 0);
        record.setTitle(title);
        record.setDescription(description);
        record.setAmount(commissionAmount);
        record.setSettlementMonth(settlementMonth);
        record.setBizNo(nextBizNo());
        earningsRecordMapper.insert(record);

        String summary = String.format("邀请奖励 +%s 创作币", commissionAmount.toPlainString());
        String content = String.format("%s\n\n收益金额：%s 创作币", description, commissionAmount.toPlainString());
        messageService.pushPersonal(userId, "reward", title, summary, null, content, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordCoinDiscountEarnings(Long userId, String sourceId, String planKey, String planName,
                                           String cycle, BigDecimal coinAmount) {
        if (coinAmount == null || coinAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("抵扣创作币数量必须大于 0");
        }

        String title = "订阅抵扣";
        String description = String.format("订阅 %s %s 抵扣 %s 创作币", planName, cycleLabel(cycle), coinAmount.toPlainString());
        String settlementMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        EarningsRecord record = new EarningsRecord();
        record.setUserId(userId);
        record.setType(EarningsType.COIN_DEDUCTION.getCode());
        record.setSourceType("subscribe_coin_discount");
        record.setSourceId(sourceId);
        record.setPlanKey(planKey);
        record.setPlanName(planName);
        record.setCycle(cycle);
        record.setTitle(title);
        record.setDescription(description);
        record.setAmount(coinAmount.negate());
        record.setSettlementMonth(settlementMonth);
        record.setBizNo(nextBizNo());
        earningsRecordMapper.insert(record);
    }

    private String cycleLabel(String cycle) {
        return switch (cycle) {
            case "month" -> "月卡";
            case "quarter" -> "季卡";
            case "year" -> "年卡";
            default -> cycle;
        };
    }

    private void doRecordEarnings(Long userId, String type, String sourceType, String sourceId,
                                  String title, String description, BigDecimal amount, String settlementMonth) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("收益金额必须大于 0");
        }
        if (!StringUtils.hasText(settlementMonth)) {
            throw new IllegalArgumentException("归属月份不能为空");
        }
        EarningsType earningsType = EarningsType.of(type);
        if (earningsType == null) {
            throw new IllegalArgumentException("收益类型非法：" + type);
        }

        EarningsRecord record = new EarningsRecord();
        record.setUserId(userId);
        record.setType(earningsType.getCode());
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        record.setTitle(title);
        record.setDescription(description);
        record.setAmount(amount);
        record.setSettlementMonth(settlementMonth);
        record.setBizNo(nextBizNo());
        earningsRecordMapper.insert(record);

        String safeTitle = StringUtils.hasText(title) ? title : "收益到账";
        String safeDescription = StringUtils.hasText(description) ? description : "";
        String summary = String.format("%s +%s 创作币", safeTitle, amount.toPlainString());
        String content = String.format("%s\n\n收益金额：%s 创作币", safeDescription, amount.toPlainString());
        messageService.pushPersonal(userId, "reward", safeTitle, summary, null, content, null);
    }

    private EarningsRecordVO toVo(EarningsRecord record) {
        EarningsRecordVO vo = new EarningsRecordVO();
        vo.setId(record.getId());
        vo.setType(record.getType());
        EarningsType typeEnum = EarningsType.of(record.getType());
        vo.setTypeLabel(typeEnum == null ? record.getType() : typeEnum.getLabel());
        vo.setTitle(record.getTitle());
        vo.setDescription(record.getDescription());
        vo.setSourceType(record.getSourceType());
        vo.setSourceId(record.getSourceId());
        vo.setSourceLabel(buildSourceLabel(record));
        vo.setFromSkillMarket("skill_market".equals(record.getSourceType()));
        vo.setBizNo(record.getBizNo());
        vo.setPlanKey(record.getPlanKey());
        vo.setPlanName(record.getPlanName());
        vo.setCycle(record.getCycle());
        vo.setOrderAmount(record.getOrderAmount());
        vo.setCommissionRate(record.getCommissionRate());
        vo.setIsFirstPurchase(record.getIsFirstPurchase());
        vo.setAmount(record.getAmount());
        vo.setSettlementMonth(record.getSettlementMonth());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }

    private String buildSourceLabel(EarningsRecord record) {
        if (!"invite".equals(record.getSourceType()) || !StringUtils.hasText(record.getSourceId())) {
            return null;
        }
        try {
            User user = userMapper.selectById(Long.valueOf(record.getSourceId()));
            if (user == null) {
                return null;
            }
            String name = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getEmail();
            String planName = StringUtils.hasText(record.getPlanName()) ? record.getPlanName() : "会员";
            return String.format("来自 %s 的 %s 订阅", name, planName);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal getCoinBalance(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null || user.getCoinBalance() == null ? BigDecimal.ZERO : user.getCoinBalance();
    }
}
