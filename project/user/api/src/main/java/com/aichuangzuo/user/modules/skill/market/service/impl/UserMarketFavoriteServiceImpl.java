package com.aichuangzuo.user.modules.skill.market.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.enums.error.SkillErrorCode;
import com.aichuangzuo.user.modules.skill.market.dto.MarketSkillRow;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.entity.UserMarketFavorite;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.user.modules.skill.market.mapper.UserMarketFavoriteMapper;
import com.aichuangzuo.user.modules.skill.market.service.UserMarketFavoriteService;
import com.aichuangzuo.user.modules.skill.market.vo.MarketSkillVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
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
    private static final int MAX_PAGE_SIZE = 100;

    private final UserMarketFavoriteMapper favoriteMapper;
    private final SkillMarketMapper skillMarketMapper;

    @Override
    public IPage<MarketSkillVO> listFavoriteSkills(Long userId, String keyword, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        String safeKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<MarketSkillRow> rowPage = new Page<>(safePage, safeSize);
        IPage<MarketSkillRow> result = favoriteMapper.selectFavoriteSkillsPage(rowPage, userId, safeKeyword);

        List<MarketSkillVO> records = result.getRecords().stream()
                .map(this::toVo)
                .collect(Collectors.toList());
        Page<MarketSkillVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    private MarketSkillVO toVo(MarketSkillRow row) {
        if (row == null || row.getSkillName() == null) {
            // 数据已被物理删除，保留占位 VO 让前端显示“已下架”
            MarketSkillVO vo = new MarketSkillVO();
            vo.setId(row == null ? null : row.getBizNo());
            vo.setName("该提示词已下架");
            vo.setStatus("offline");
            vo.setSourceType("admin");
            vo.setPrice(BigDecimal.ZERO);
            return vo;
        }

        MarketSkillVO vo = new MarketSkillVO();
        vo.setId(row.getBizNo());
        vo.setName(row.getSkillName());
        vo.setDescription(row.getDescription());
        vo.setSourceType(toSourceTypeString(row.getSourceType()));
        vo.setCreatorId(row.getPublisherUserId());
        vo.setCreatorName(row.getPublisherName());
        vo.setPrompt(row.getPrompt());
        vo.setScope(row.getScope());
        vo.setExcerpt1(null);
        vo.setExcerpt2(null);
        vo.setStatus(resolveStatus(row.getEnableStatus(), row.getAuditStatus(), row.getIsDeleted()));
        vo.setPrice(row.getPrice());
        vo.setWeeklyUses(row.getWeeklyUses());
        vo.setTotalUses(row.getTotalUses());
        vo.setWeeklyEarnings(row.getWeeklyEarnings());
        vo.setMilestoneBonus(row.getMilestoneBonus());
        vo.setFeatured(row.getFeatured() != null && row.getFeatured() == 1);
        vo.setLastSettlementAt(row.getLastSettlementAt());
        vo.setApprovedAt(row.getApprovedAt());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    private String toSourceTypeString(Integer code) {
        return code != null && code == 1 ? "my" : "learned";
    }

    private String resolveStatus(Integer enableStatus, Integer auditStatus, Integer isDeleted) {
        if (isDeleted != null && isDeleted == 1) {
            return "offline";
        }
        if (enableStatus == null || enableStatus != ENABLE_ENABLED) {
            return "offline";
        }
        if (auditStatus == null) {
            return "pending";
        }
        return switch (auditStatus) {
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
