package com.aichuangzuo.user.modules.skill.market.job;

import com.aichuangzuo.user.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.user.modules.earnings.enums.EarningsType;
import com.aichuangzuo.user.modules.earnings.mapper.EarningsRecordMapper;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.skill.market.config.entity.SkillMonthlyRewardConfig;
import com.aichuangzuo.user.modules.skill.market.config.mapper.SkillMonthlyRewardConfigMapper;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提示词市场月度结算定时任务。
 *
 * <p>每月 1 日 03:00 执行：
 * <ol>
 *   <li>按当月收益取 Top5 创作者；</li>
 *   <li>发放排行榜奖励（创作币入账 + 收益记录）；</li>
 *   <li>将上月 USAGE 收益记录标记为已结算；</li>
 *   <li>清零所有市场提示词的月度统计字段。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillMarketMonthlySettlementJob {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String SOURCE_TYPE_SKILL_MARKET = "skill_market";
    private static final String BIZ_TYPE_SKILL_MONTHLY_LEADERBOARD = "SKILL_MONTHLY_LEADERBOARD";
    private static final int TOP_N = 5;

    private final SkillMonthlyRewardConfigMapper configMapper;
    private final SkillMarketMapper skillMarketMapper;
    private final EarningsRecordMapper earningsRecordMapper;
    private final CoinRecordService coinRecordService;

    /**
     * 默认每月 1 日 03:00 执行。具体 cron 以管理端配置为准，修改后需重启服务生效。
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    @Transactional(rollbackFor = Exception.class)
    public void settle() {
        SkillMonthlyRewardConfig config = configMapper.selectById(1L);
        if (config == null || config.getEnabled() == null || config.getEnabled() != 1) {
            log.info("提示词市场月度奖励配置未启用，跳过结算");
            return;
        }

        String targetMonth = LocalDate.now().minusMonths(1).format(MONTH_FMT);
        log.info("开始提示词市场月度结算 targetMonth={}", targetMonth);

        List<BigDecimal> rewards = List.of(
                config.getFirstAmount(),
                config.getSecondAmount(),
                config.getThirdAmount(),
                config.getFourthAmount(),
                config.getFifthAmount()
        );

        // 1. 聚合当前月度收益，取 Top5
        List<SkillMarket> approvedSkills = skillMarketMapper.selectList(
                new LambdaQueryWrapper<SkillMarket>()
                        .eq(SkillMarket::getAuditStatus, 1)
                        .eq(SkillMarket::getIsDeleted, 0));

        Map<Long, List<SkillMarket>> byPublisher = approvedSkills.stream()
                .filter(s -> s.getPublisherUserId() != null)
                .collect(Collectors.groupingBy(SkillMarket::getPublisherUserId));

        List<TopCreator> topCreators = new ArrayList<>();
        byPublisher.forEach((publisherId, skills) -> {
            BigDecimal monthlyEarnings = skills.stream()
                    .map(s -> s.getMonthlyEarnings() == null ? BigDecimal.ZERO : s.getMonthlyEarnings())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            topCreators.add(new TopCreator(publisherId, monthlyEarnings, skills));
        });

        topCreators.sort(Comparator.comparing(TopCreator::monthlyEarnings, Comparator.reverseOrder()));
        List<TopCreator> winners = topCreators.stream().limit(TOP_N).toList();

        // 2. 发放排行榜奖励
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < winners.size(); i++) {
            TopCreator winner = winners.get(i);
            int rank = i + 1;
            BigDecimal amount = rewards.get(i);
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String refId = BIZ_TYPE_SKILL_MONTHLY_LEADERBOARD + "_" + targetMonth + "_RANK" + rank + "_" + winner.publisherId();

            boolean alreadyGranted = earningsRecordMapper.selectCount(
                    new LambdaQueryWrapper<EarningsRecord>()
                            .eq(EarningsRecord::getUserId, winner.publisherId())
                            .eq(EarningsRecord::getType, EarningsType.LEADERBOARD_REWARD.getCode())
                            .eq(EarningsRecord::getSourceId, refId)
                            .eq(EarningsRecord::getSettlementMonth, targetMonth)) > 0;
            if (alreadyGranted) {
                log.info("月度排行榜奖励已发放，跳过 refId={}", refId);
                continue;
            }

            coinRecordService.grant(winner.publisherId(), BIZ_TYPE_SKILL_MONTHLY_LEADERBOARD, amount, refId,
                    targetMonth + " 月提示词市场收益榜 Top" + rank + " 奖励");

            EarningsRecord rewardRecord = new EarningsRecord();
            rewardRecord.setUserId(winner.publisherId());
            rewardRecord.setType(EarningsType.LEADERBOARD_REWARD.getCode());
            rewardRecord.setSourceType(SOURCE_TYPE_SKILL_MARKET);
            rewardRecord.setSourceId(refId);
            rewardRecord.setTitle(targetMonth + " 月提示词市场 Top" + rank + " 奖励");
            rewardRecord.setDescription("当月收益榜排名第 " + rank + " 名，获得奖励 " + amount + " 创作币");
            rewardRecord.setAmount(amount);
            rewardRecord.setStatus(1);
            rewardRecord.setSettlementMonth(targetMonth);
            rewardRecord.setSettledAt(now);
            earningsRecordMapper.insert(rewardRecord);

            // 把该创作者名下所有技能的 leaderboard_reward 标记为本次奖励金额
            List<Long> skillIds = winner.skills().stream().map(SkillMarket::getId).toList();
            if (!skillIds.isEmpty()) {
                skillMarketMapper.update(null, new LambdaUpdateWrapper<SkillMarket>()
                        .in(SkillMarket::getId, skillIds)
                        .set(SkillMarket::getLeaderboardReward, amount)
                        .set(SkillMarket::getUpdatedAt, now));
            }

            log.info("发放月度排行榜奖励 rank={} publisherId={} amount={} refId={}", rank, winner.publisherId(), amount, refId);
        }

        // 3. 将上月 USAGE 记录标记为已结算
        earningsRecordMapper.update(null, new LambdaUpdateWrapper<EarningsRecord>()
                .eq(EarningsRecord::getType, EarningsType.USAGE.getCode())
                .eq(EarningsRecord::getSettlementMonth, targetMonth)
                .eq(EarningsRecord::getStatus, 0)
                .set(EarningsRecord::getStatus, 1)
                .set(EarningsRecord::getSettledAt, now));

        // 4. 清零月度统计字段
        skillMarketMapper.update(null, new LambdaUpdateWrapper<SkillMarket>()
                .eq(SkillMarket::getIsDeleted, 0)
                .set(SkillMarket::getMonthlyUses, 0)
                .set(SkillMarket::getMonthlyEarnings, BigDecimal.ZERO)
                .set(SkillMarket::getLeaderboardReward, BigDecimal.ZERO)
                .set(SkillMarket::getLastSettlementAt, now.truncatedTo(ChronoUnit.MILLIS)));

        log.info("提示词市场月度结算完成 targetMonth={} winners={}", targetMonth, winners.size());
    }

    private record TopCreator(Long publisherId, BigDecimal monthlyEarnings, List<SkillMarket> skills) {
    }
}
