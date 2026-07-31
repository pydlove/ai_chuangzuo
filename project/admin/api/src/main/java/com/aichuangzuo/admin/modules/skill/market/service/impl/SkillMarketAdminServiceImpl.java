package com.aichuangzuo.admin.modules.skill.market.service.impl;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.skill.market.dto.SkillMarketRow;
import com.aichuangzuo.admin.modules.skill.market.dto.request.CreateSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.SkillMarketPageRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.UpdateSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.admin.modules.skill.market.enums.AdminSkillMarketErrorCode;
import com.aichuangzuo.admin.modules.skill.market.mapper.SkillMarketAggregateMapper;
import com.aichuangzuo.admin.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.admin.modules.skill.market.service.SkillMarketAdminService;
import com.aichuangzuo.admin.modules.skill.market.vo.SkillMarketVO;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 管理端 - 风格市场服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillMarketAdminServiceImpl implements SkillMarketAdminService {

    private static final int AUDIT_STATUS_APPROVED = 1;
    private static final int SOURCE_TYPE_PLATFORM = 3;

    private final SkillMarketMapper skillMarketMapper;
    private final SkillMarketAggregateMapper aggregateMapper;
    private final PlatformUserMapper platformUserMapper;

    @Override
    public IPage<SkillMarketVO> page(SkillMarketPageRequest request) {
        long offset = (long) (request.getPageNum() - 1) * request.getPageSize();
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;

        List<SkillMarketRow> rows = aggregateMapper.selectMarketStylePage(
                request.getEnableStatus(), keyword, offset, request.getPageSize());
        long total = aggregateMapper.countMarketStylePage(request.getEnableStatus(), keyword);

        List<SkillMarketVO> records = rows.stream().map(this::toVo).toList();

        Page<SkillMarketVO> page = new Page<>(request.getPageNum(), request.getPageSize());
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CreateSkillMarketRequest request) {
        validateName(request.getSkillName());
        validateEnableStatus(request.getEnableStatus());
        validateFeatured(request.getFeatured());
        validatePublisher(request.getPublisherUserId());
        validateTotalUses(request.getTotalUses());

        SkillMarket market = new SkillMarket();
        market.setBizNo(generateBizNo());
        market.setSkillName(request.getSkillName().trim());
        market.setDescription(StringUtils.trimWhitespace(request.getDescription()));
        market.setPromptSummary(StringUtils.trimWhitespace(request.getPromptSummary()));
        market.setPrompt(request.getPrompt().trim());
        market.setScope(normalizeScope(request.getScope()));
        market.setPublisherUserId(request.getPublisherUserId());
        market.setTotalUses(request.getTotalUses() != null ? request.getTotalUses() : 0);
        market.setWeeklyUses(0);
        market.setWeeklyEarnings(BigDecimal.ZERO);
        market.setMilestoneBonus(BigDecimal.ZERO);
        market.setEnableStatus(request.getEnableStatus());
        market.setFeatured(request.getFeatured());
        market.setAuditStatus(AUDIT_STATUS_APPROVED);
        market.setSourceType(SOURCE_TYPE_PLATFORM);
        market.setIsDeleted(0);

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId != null) {
            market.setCreatedBy(adminId);
            market.setUpdatedBy(adminId);
        }

        skillMarketMapper.insert(market);
        log.info("创建风格市场条目 bizNo={}, name={}", market.getBizNo(), market.getSkillName());
        return market.getBizNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String bizNo, UpdateSkillMarketRequest request) {
        validateEnableStatus(request.getEnableStatus());
        validateFeatured(request.getFeatured());
        validateTotalUses(request.getTotalUses());

        SkillMarket market = loadByBizNo(bizNo);

        String newName = request.getSkillName().trim();
        if (!newName.equals(market.getSkillName())) {
            ensureNameNotExists(newName, bizNo);
        }

        validatePublisher(request.getPublisherUserId());

        market.setSkillName(newName);
        market.setDescription(StringUtils.trimWhitespace(request.getDescription()));
        market.setPromptSummary(StringUtils.trimWhitespace(request.getPromptSummary()));
        market.setPrompt(request.getPrompt().trim());
        market.setScope(normalizeScope(request.getScope()));
        market.setPublisherUserId(request.getPublisherUserId());
        market.setTotalUses(request.getTotalUses() != null ? request.getTotalUses() : 0);
        market.setEnableStatus(request.getEnableStatus());
        market.setFeatured(request.getFeatured());

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId != null) {
            market.setUpdatedBy(adminId);
        }

        skillMarketMapper.updateById(market);
        log.info("更新风格市场条目 bizNo={}, name={}, enableStatus={}",
                bizNo, newName, request.getEnableStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String bizNo) {
        SkillMarket market = loadByBizNo(bizNo);
        market.setIsDeleted(1);
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId != null) {
            market.setUpdatedBy(adminId);
        }
        skillMarketMapper.updateById(market);
        log.info("软删风格市场条目 bizNo={}", bizNo);
    }

    // -------- helpers --------

    private SkillMarket loadByBizNo(String bizNo) {
        LambdaQueryWrapper<SkillMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillMarket::getBizNo, bizNo)
                .eq(SkillMarket::getIsDeleted, 0);
        SkillMarket market = skillMarketMapper.selectOne(wrapper);
        if (market == null) {
            throw new BusinessException(AdminSkillMarketErrorCode.SKILL_MARKET_NOT_FOUND);
        }
        return market;
    }

    private void validateName(String name) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(name.trim())) {
            throw new BusinessException(AdminSkillMarketErrorCode.SKILL_MARKET_NAME_EXISTS);
        }
        ensureNameNotExists(name.trim(), null);
    }

    private void ensureNameNotExists(String name, String excludeBizNo) {
        LambdaQueryWrapper<SkillMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillMarket::getSkillName, name)
                .eq(SkillMarket::getIsDeleted, 0);
        if (excludeBizNo != null) {
            wrapper.ne(SkillMarket::getBizNo, excludeBizNo);
        }
        Long count = skillMarketMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(AdminSkillMarketErrorCode.SKILL_MARKET_NAME_EXISTS);
        }
    }

    private void validateEnableStatus(Integer enableStatus) {
        if (enableStatus == null || (enableStatus != 0 && enableStatus != 1)) {
            throw new BusinessException(AdminSkillMarketErrorCode.ENABLE_STATUS_INVALID);
        }
    }

    private void validateFeatured(Integer featured) {
        if (featured == null || (featured != 0 && featured != 1)) {
            throw new BusinessException(AdminSkillMarketErrorCode.FEATURED_STATUS_INVALID);
        }
    }

    private void validatePublisher(Long publisherUserId) {
        if (publisherUserId == null) {
            throw new BusinessException(AdminSkillMarketErrorCode.PUBLISHER_NOT_FOUND);
        }
        PlatformUser user = platformUserMapper.selectById(publisherUserId);
        if (user == null || user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            throw new BusinessException(AdminSkillMarketErrorCode.PUBLISHER_NOT_FOUND);
        }
    }

    private void validateTotalUses(Integer totalUses) {
        if (totalUses != null && totalUses < 0) {
            throw new BusinessException(AdminSkillMarketErrorCode.TOTAL_USES_INVALID);
        }
    }

    private String normalizeScope(String scope) {
        if (scope == null) {
            return null;
        }
        String trimmed = scope.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateBizNo() {
        return "SM" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private SkillMarketVO toVo(SkillMarketRow row) {
        SkillMarketVO vo = new SkillMarketVO();
        vo.setId(row.getBizNo());
        vo.setName(row.getSkillName());
        vo.setDescription(row.getDescription());
        vo.setPromptSummary(row.getPromptSummary());
        vo.setPrompt(row.getPrompt());
        vo.setScope(row.getScope());
        vo.setPublisherUserId(row.getPublisherUserId());
        vo.setPublisherName(row.getPublisherName());
        vo.setTotalUses(row.getTotalUses());
        vo.setWeeklyUses(row.getWeeklyUses());
        vo.setWeeklyEarnings(row.getWeeklyEarnings());
        vo.setMilestoneBonus(row.getMilestoneBonus());
        vo.setStatus(row.getEnableStatus() != null && row.getEnableStatus() == 1 ? "enabled" : "disabled");
        vo.setFeatured(row.getFeatured() != null && row.getFeatured() == 1 ? 1 : 0);
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }
}
