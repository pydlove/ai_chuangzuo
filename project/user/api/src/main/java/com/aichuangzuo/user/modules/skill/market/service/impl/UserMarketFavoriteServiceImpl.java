package com.aichuangzuo.user.modules.skill.market.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.skill.enums.SkillErrorCode;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.entity.UserMarketFavorite;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.user.modules.skill.market.mapper.UserMarketFavoriteMapper;
import com.aichuangzuo.user.modules.skill.market.service.UserMarketFavoriteService;
import com.aichuangzuo.user.modules.skill.market.vo.MarketSkillVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户风格市场收藏服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMarketFavoriteServiceImpl implements UserMarketFavoriteService {

    private static final int ENABLE_ENABLED = 1;
    private static final int AUDIT_APPROVED = 1;

    private final UserMarketFavoriteMapper favoriteMapper;
    private final SkillMarketMapper skillMarketMapper;
    private final UserMapper userMapper;

    @Override
    public List<MarketSkillVO> listFavoriteSkills(Long userId) {
        LambdaQueryWrapper<UserMarketFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMarketFavorite::getUserId, userId)
                .orderByDesc(UserMarketFavorite::getCreatedAt);
        List<UserMarketFavorite> favorites = favoriteMapper.selectList(wrapper);
        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        List<MarketSkillVO> vos = new ArrayList<>(favorites.size());
        for (UserMarketFavorite favorite : favorites) {
            vos.add(buildFavoriteSkillVo(favorite.getMarketSkillId()));
        }

        Set<Long> creatorIds = vos.stream()
                .map(MarketSkillVO::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!creatorIds.isEmpty()) {
            Map<Long, String> creatorNameMap = userMapper.selectBatchIds(creatorIds).stream()
                    .collect(Collectors.toMap(
                            User::getId,
                            u -> StringUtils.hasText(u.getNickname()) ? u.getNickname() : "用户" + u.getId()
                    ));
            for (MarketSkillVO vo : vos) {
                if (vo.getCreatorId() != null && creatorNameMap.containsKey(vo.getCreatorId())) {
                    vo.setCreatorName(creatorNameMap.get(vo.getCreatorId()));
                }
            }
        }

        return vos.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private MarketSkillVO buildFavoriteSkillVo(String marketSkillId) {
        SkillMarket market = skillMarketMapper.selectByBizNoIncludeDeleted(marketSkillId);
        if (market == null) {
            // 数据已被物理删除，保留占位 VO 让前端显示“已下架”
            MarketSkillVO vo = new MarketSkillVO();
            vo.setId(marketSkillId);
            vo.setName("该提示词已下架");
            vo.setStatus("offline");
            vo.setSourceType("admin");
            vo.setPrice(BigDecimal.ZERO);
            return vo;
        }
        return toVo(market);
    }

    private MarketSkillVO toVo(SkillMarket market) {
        MarketSkillVO vo = new MarketSkillVO();
        vo.setId(market.getBizNo());
        vo.setName(market.getSkillName());
        vo.setDescription(market.getDescription());
        vo.setSourceType(toSourceTypeString(market.getSourceType()));
        vo.setCreatorId(market.getPublisherUserId());
        vo.setPrompt(market.getPrompt());
        vo.setScope(market.getScope());
        vo.setExcerpt1(null);
        vo.setExcerpt2(null);
        vo.setStatus(resolveStatus(market));
        vo.setPrice(market.getPrice());
        vo.setWeeklyUses(market.getWeeklyUses());
        vo.setTotalUses(market.getTotalUses());
        vo.setWeeklyEarnings(market.getWeeklyEarnings());
        vo.setMilestoneBonus(market.getMilestoneBonus());
        vo.setFeatured(Boolean.FALSE);
        vo.setLastSettlementAt(market.getLastSettlementAt());
        vo.setCreatedAt(market.getCreatedAt());
        return vo;
    }

    private String toSourceTypeString(Integer code) {
        return code != null && code == 1 ? "my" : "learned";
    }

    private String resolveStatus(SkillMarket market) {
        if (market.getIsDeleted() != null && market.getIsDeleted() == 1) {
            return "offline";
        }
        if (market.getEnableStatus() == null || market.getEnableStatus() != ENABLE_ENABLED) {
            return "offline";
        }
        if (market.getAuditStatus() == null) {
            return "pending";
        }
        return switch (market.getAuditStatus()) {
            case 1 -> "approved";
            case 2 -> "rejected";
            default -> "pending";
        };
    }

    @Override
    public void addFavorite(Long userId, String marketSkillId) {
        if (!StringUtils.hasText(marketSkillId)) {
            throw new BusinessException(SkillErrorCode.SKILL_NOT_FOUND);
        }
        ensureMarketSkillExists(marketSkillId);

        LambdaQueryWrapper<UserMarketFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMarketFavorite::getUserId, userId)
                .eq(UserMarketFavorite::getMarketSkillId, marketSkillId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            return;
        }

        UserMarketFavorite favorite = new UserMarketFavorite();
        favorite.setUserId(userId);
        favorite.setMarketSkillId(marketSkillId);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        log.info("收藏市场 skill userId={}, marketSkillId={}", userId, marketSkillId);
    }

    @Override
    public void removeFavorite(Long userId, String marketSkillId) {
        if (!StringUtils.hasText(marketSkillId)) {
            return;
        }
        LambdaQueryWrapper<UserMarketFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMarketFavorite::getUserId, userId)
                .eq(UserMarketFavorite::getMarketSkillId, marketSkillId);
        favoriteMapper.delete(wrapper);
        log.info("取消收藏市场 skill userId={}, marketSkillId={}", userId, marketSkillId);
    }

    private void ensureMarketSkillExists(String marketSkillId) {
        LambdaQueryWrapper<SkillMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillMarket::getBizNo, marketSkillId)
                .eq(SkillMarket::getEnableStatus, ENABLE_ENABLED)
                .eq(SkillMarket::getAuditStatus, AUDIT_APPROVED);
        Long count = skillMarketMapper.selectCount(wrapper);
        if (count == null || count == 0) {
            throw new BusinessException(SkillErrorCode.SKILL_NOT_FOUND);
        }
    }
}
