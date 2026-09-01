package com.aichuangzuo.user.modules.skill.market.service;

import com.aichuangzuo.user.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.user.modules.earnings.enums.EarningsType;
import com.aichuangzuo.user.modules.earnings.mapper.EarningsRecordMapper;
import com.aichuangzuo.user.modules.earnings.service.EarningsService;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 提示词市场使用与结算服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillMarketUsageService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String SOURCE_TYPE_SKILL_MARKET = "skill_market";
    private static final BigDecimal DEFAULT_PRICE_PER_USE = new BigDecimal("2.00");

    private final SkillMarketMapper skillMarketMapper;
    private final EarningsRecordMapper earningsRecordMapper;
    private final EarningsService earningsService;
    private final CoinRecordService coinRecordService;

    /**
     * 记录一次市场提示词被使用（生成文章成功后调用）。
     *
     * <p>增加累计使用次数与收益，并写入未结算的 USAGE 收益记录。
     *
     * @param marketSkillBizNo 市场提示词业务编号
     * @param consumerUserId   使用者用户ID（用于日志，不参与分成计算）
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordUsage(String marketSkillBizNo, Long consumerUserId) {
        SkillMarket skill = skillMarketMapper.selectOne(
                new LambdaQueryWrapper<SkillMarket>()
                        .eq(SkillMarket::getBizNo, marketSkillBizNo)
                        .eq(SkillMarket::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (skill == null) {
            log.warn("记录提示词使用失败，skill 不存在 bizNo={}", marketSkillBizNo);
            return;
        }
        if (skill.getAuditStatus() == null || skill.getAuditStatus() != 1) {
            log.info("提示词未通过审核，不计入使用收益 bizNo={} auditStatus={}", marketSkillBizNo, skill.getAuditStatus());
            return;
        }

        BigDecimal price = DEFAULT_PRICE_PER_USE;
        String month = LocalDateTime.now().format(MONTH_FMT);

        skillMarketMapper.incrementUsageStats(skill.getId(), price);

        String description = "用户 " + consumerUserId + " 使用「" + skill.getSkillName() + "」生成文章";

        coinRecordService.grant(skill.getPublisherUserId(), "skill_market_usage", price,
                marketSkillBizNo, "提示词使用收益：" + skill.getSkillName());

        EarningsRecord record = new EarningsRecord();
        record.setUserId(skill.getPublisherUserId());
        record.setType(EarningsType.USAGE.getCode());
        record.setSourceType(SOURCE_TYPE_SKILL_MARKET);
        record.setSourceId(marketSkillBizNo);
        record.setTitle("「" + skill.getSkillName() + "」被使用");
        record.setDescription(description);
        record.setAmount(price);
        record.setStatus(0);
        record.setSettlementMonth(month);
        record.setBizNo(earningsService.nextBizNo());
        earningsRecordMapper.insert(record);

        log.info("记录提示词市场使用 bizNo={} skillName={} consumer={} publisher={} price={} month={}",
                marketSkillBizNo, skill.getSkillName(), consumerUserId, skill.getPublisherUserId(), price, month);
    }
}
