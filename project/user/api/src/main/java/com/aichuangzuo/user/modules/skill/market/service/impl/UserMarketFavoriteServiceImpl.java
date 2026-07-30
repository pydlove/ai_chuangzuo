package com.aichuangzuo.user.modules.skill.market.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.skill.enums.SkillErrorCode;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.entity.UserMarketFavorite;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.user.modules.skill.market.mapper.UserMarketFavoriteMapper;
import com.aichuangzuo.user.modules.skill.market.service.UserMarketFavoriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    public List<String> listFavoriteIds(Long userId) {
        LambdaQueryWrapper<UserMarketFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMarketFavorite::getUserId, userId)
                .orderByDesc(UserMarketFavorite::getCreatedAt);
        return favoriteMapper.selectList(wrapper).stream()
                .map(UserMarketFavorite::getMarketSkillId)
                .collect(Collectors.toList());
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
