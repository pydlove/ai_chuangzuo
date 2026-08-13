package com.aichuangzuo.admin.modules.leaderboard.config.service;

import com.aichuangzuo.admin.modules.leaderboard.config.dto.request.LeaderboardRewardConfigUpdateRequest;
import com.aichuangzuo.admin.modules.leaderboard.config.entity.LeaderboardRewardConfig;
import com.aichuangzuo.admin.modules.leaderboard.config.mapper.LeaderboardRewardConfigMapper;
import com.aichuangzuo.admin.modules.leaderboard.config.vo.LeaderboardRewardConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 收益排行榜奖励规则配置服务。
 *
 * <p>单行配置（id=1），admin 端 GET/PUT 维护。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardRewardConfigService {

    private static final long CONFIG_ID = 1L;
    private static final int DEFAULT_TOP_LIMIT = 3;
    private static final BigDecimal DEFAULT_REWARD_AMOUNT = new BigDecimal("500.0000");

    private final LeaderboardRewardConfigMapper mapper;

    public LeaderboardRewardConfigVO detail() {
        LeaderboardRewardConfig config = mapper.selectById(CONFIG_ID);
        if (config == null) {
            config = new LeaderboardRewardConfig();
            config.setId(CONFIG_ID);
            config.setRewardTopLimit(DEFAULT_TOP_LIMIT);
            config.setRewardAmount(DEFAULT_REWARD_AMOUNT);
        }
        return toVo(config);
    }

    /**
     * 获取当前生效的配置实体（用于发奖逻辑）。
     */
    public LeaderboardRewardConfig getEffectiveConfig() {
        LeaderboardRewardConfig config = mapper.selectById(CONFIG_ID);
        if (config == null) {
            config = new LeaderboardRewardConfig();
            config.setId(CONFIG_ID);
            config.setRewardTopLimit(DEFAULT_TOP_LIMIT);
            config.setRewardAmount(DEFAULT_REWARD_AMOUNT);
        }
        return config;
    }

    @Transactional(rollbackFor = Exception.class)
    public LeaderboardRewardConfigVO update(LeaderboardRewardConfigUpdateRequest req, Long adminUserId) {
        LeaderboardRewardConfig exist = mapper.selectById(CONFIG_ID);
        boolean isNew = exist == null;
        if (isNew) {
            exist = new LeaderboardRewardConfig();
            exist.setId(CONFIG_ID);
        }
        exist.setRewardTopLimit(req.getTopLimit());
        exist.setRewardAmount(req.getRewardAmount());
        exist.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        if (isNew) {
            exist.setCreatedBy(adminUserId == null ? 0L : adminUserId);
            mapper.insert(exist);
        } else {
            mapper.updateById(exist);
        }

        log.info("admin={} 更新收益排行榜奖励规则 topLimit={} rewardAmount={}",
                adminUserId, exist.getRewardTopLimit(), exist.getRewardAmount());
        return toVo(exist);
    }

    private LeaderboardRewardConfigVO toVo(LeaderboardRewardConfig config) {
        LeaderboardRewardConfigVO vo = new LeaderboardRewardConfigVO();
        vo.setTopLimit(config.getRewardTopLimit());
        vo.setRewardAmount(config.getRewardAmount());
        return vo;
    }
}
