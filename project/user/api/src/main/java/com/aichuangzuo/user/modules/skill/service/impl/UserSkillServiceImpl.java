package com.aichuangzuo.user.modules.skill.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.benefit.entity.PlanBenefit;
import com.aichuangzuo.user.modules.benefit.mapper.PlanBenefitMapper;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.skill.dto.request.CreateSkillRequest;
import com.aichuangzuo.user.modules.skill.dto.request.UpdateSkillRequest;
import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.skill.enums.SkillErrorCode;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.user.modules.skill.service.UserSkillService;
import com.aichuangzuo.user.modules.skill.vo.UserSkillVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户风格服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSkillServiceImpl implements UserSkillService {

    private static final int MAX_SCOPE_TAGS = 3;
    private static final int MAX_SCOPE_TAG_LENGTH = 8;
    private static final int SOURCE_TYPE_CUSTOM = 1;
    private static final int SOURCE_TYPE_LEARNED = 2;
    private static final String BENEFIT_CODE_STYLE_CUSTOM = "skill_custom";
    private static final String BENEFIT_CODE_LEARN_ANALYZE = "skill_learn_analyze";
    private static final String BENEFIT_CODE_SKILL_MARKET_PUBLISH = "skill_market_publish";
    private static final int AUDIT_STATUS_PENDING = 0;
    private static final int AUDIT_STATUS_APPROVED = 1;
    private static final int ENABLE_STATUS_DISABLED = 0;
    private static final int ENABLE_STATUS_ENABLED = 1;
    private static final int NOT_DELETED = 0;
    private static final BigDecimal DEFAULT_PRICE_PER_USE = new BigDecimal("2.00");

    private final UserSkillMapper userSkillMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final PlanBenefitMapper planBenefitMapper;
    private final SkillMarketMapper skillMarketMapper;
    private final BenefitService benefitService;

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 12;

    @Override
    public IPage<UserSkillVO> listMySkills(Integer sourceType, String keyword, int page, int pageSize) {
        Long userId = SecurityUserContext.getCurrentUserId();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);

        LambdaQueryWrapper<UserSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSourceType, sourceType == null ? SOURCE_TYPE_CUSTOM : sourceType)
                .orderByDesc(UserSkill::getUpdatedAt);
        if (StringUtils.hasText(keyword)) {
            String q = keyword.trim();
            wrapper.and(w -> w.like(UserSkill::getSkillName, q)
                    .or()
                    .like(UserSkill::getScope, q)
                    .or()
                    .like(UserSkill::getPrompt, q)
                    .or()
                    .like(UserSkill::getDescription, q));
        }
        Page<UserSkill> rowPage = new Page<>(safePage, safeSize);
        IPage<UserSkill> result = userSkillMapper.selectPage(rowPage, wrapper);
        List<UserSkillVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        Page<UserSkillVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public UserSkillVO createSkill(CreateSkillRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        String skillName = request.getSkillName().trim();
        String prompt = request.getPrompt().trim();
        String scope = normalizeScope(request.getScope());
        String description = request.getDescription() == null ? null : request.getDescription().trim();
        String excerpt1 = request.getExcerpt1() == null ? null : request.getExcerpt1().trim();
        String excerpt2 = request.getExcerpt2() == null ? null : request.getExcerpt2().trim();

        validateScope(scope);
        int sourceType = request.getSourceType() == null ? SOURCE_TYPE_CUSTOM : request.getSourceType();
        ensureNameUniqueGlobally(skillName, null);
        if (sourceType == SOURCE_TYPE_CUSTOM) {
            ensureSkillQuotaNotExceeded(userId);
        }

        if (sourceType == SOURCE_TYPE_LEARNED) {
            try {
                benefitService.consume(userId, BENEFIT_CODE_LEARN_ANALYZE);
            } catch (BusinessException e) {
                throw new BusinessException(SkillErrorCode.SKILL_LEARN_QUOTA_EXCEEDED);
            }
        }

        UserSkill skill = new UserSkill();
        skill.setBizNo(generateBizNo());
        skill.setUserId(userId);
        skill.setSkillName(skillName);
        skill.setPrompt(prompt);
        skill.setExcerpt1(excerpt1);
        skill.setExcerpt2(excerpt2);
        skill.setDescription(description);
        skill.setScope(scope);
        skill.setSourceType(sourceType);
        skill.setAuditStatus(0);
        skill.setUseCount(0);

        userSkillMapper.insert(skill);
        log.info("创建风格成功 userId={}, bizNo={}, skillName={}", userId, skill.getBizNo(), skillName);
        return toVO(skill);
    }

    @Override
    public UserSkillVO updateSkill(String bizNo, UpdateSkillRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        UserSkill skill = getOwnedSkill(bizNo, userId);

        String skillName = request.getSkillName().trim();
        String prompt = request.getPrompt().trim();
        String scope = normalizeScope(request.getScope());
        String description = request.getDescription() == null ? null : request.getDescription().trim();
        String excerpt1 = request.getExcerpt1() == null ? null : request.getExcerpt1().trim();
        String excerpt2 = request.getExcerpt2() == null ? null : request.getExcerpt2().trim();

        validateScope(scope);
        ensureNameUniqueGlobally(skillName, skill.getId());

        skill.setSkillName(skillName);
        skill.setPrompt(prompt);
        skill.setExcerpt1(excerpt1);
        skill.setExcerpt2(excerpt2);
        skill.setDescription(description);
        skill.setScope(scope);
        // 修改后重新进入待审核状态，并清空上一次的打回原因
        skill.setAuditStatus(0);
        skill.setRejectReason(null);

        userSkillMapper.updateById(skill);
        syncSkillToPendingMarket(skill);
        log.info("更新风格成功 userId={}, bizNo={}, skillName={}", userId, bizNo, skillName);
        return toVO(skill);
    }

    /**
     * 如果存在对应的市场待审核记录，同步更新其内容，避免审核员看到的是旧版本。
     */
    private void syncSkillToPendingMarket(UserSkill skill) {
        LambdaQueryWrapper<SkillMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillMarket::getBizNo, skill.getBizNo())
                .eq(SkillMarket::getIsDeleted, 0);
        SkillMarket market = skillMarketMapper.selectOne(wrapper);
        if (market == null) {
            return;
        }
        market.setSkillName(skill.getSkillName());
        market.setDescription(skill.getDescription());
        market.setPromptSummary(skill.getPromptSummary());
        market.setPrompt(skill.getPrompt());
        market.setScope(skill.getScope());
        market.setUpdatedAt(LocalDateTime.now());
        skillMarketMapper.updateById(market);
        log.info("同步更新市场待审核记录 bizNo={}", skill.getBizNo());
    }

    @Override
    public void deleteSkill(String bizNo) {
        Long userId = SecurityUserContext.getCurrentUserId();
        UserSkill skill = getOwnedSkill(bizNo, userId);
        userSkillMapper.deleteById(skill.getId());
        log.info("删除风格成功 userId={}, bizNo={}", userId, bizNo);
    }

    @Override
    public void publishSkill(String bizNo) {
        Long userId = SecurityUserContext.getCurrentUserId();
        UserSkill skill = getOwnedSkill(bizNo, userId);

        try {
            benefitService.consume(userId, BENEFIT_CODE_SKILL_MARKET_PUBLISH);
        } catch (BusinessException e) {
            throw new BusinessException(SkillErrorCode.SKILL_MARKET_PUBLISH_QUOTA_EXCEEDED);
        }

        skill.setAuditStatus(AUDIT_STATUS_PENDING);
        skill.setRejectReason(null);
        userSkillMapper.updateById(skill);

        SkillMarket market = skillMarketMapper.selectByBizNoIncludeDeleted(bizNo);
        BigDecimal price = DEFAULT_PRICE_PER_USE;
        if (market != null) {
            market.setSkillName(skill.getSkillName());
            market.setDescription(skill.getDescription());
            market.setPromptSummary(skill.getPromptSummary());
            market.setPrompt(skill.getPrompt());
            market.setScope(skill.getScope());
            market.setPublisherUserId(userId);
            market.setPrice(price);
            market.setEnableStatus(ENABLE_STATUS_DISABLED);
            market.setAuditStatus(AUDIT_STATUS_PENDING);
            market.setSourceType(skill.getSourceType());
            market.setIsDeleted(NOT_DELETED);
            market.setUpdatedAt(LocalDateTime.now());
            skillMarketMapper.updateById(market);
            log.info("重新提交市场 skill 审核 userId={}, bizNo={}", userId, bizNo);
        } else {
            market = new SkillMarket();
            market.setBizNo(skill.getBizNo());
            market.setSkillName(skill.getSkillName());
            market.setDescription(skill.getDescription());
            market.setPromptSummary(skill.getPromptSummary());
            market.setPrompt(skill.getPrompt());
            market.setScope(skill.getScope());
            market.setPublisherUserId(userId);
            market.setPrice(price);
            market.setTotalUses(0);
            market.setWeeklyUses(0);
            market.setWeeklyEarnings(BigDecimal.ZERO);
            market.setMilestoneBonus(BigDecimal.ZERO);
            market.setEnableStatus(ENABLE_STATUS_DISABLED);
            market.setAuditStatus(AUDIT_STATUS_PENDING);
            market.setSourceType(skill.getSourceType());
            market.setIsDeleted(NOT_DELETED);
            skillMarketMapper.insert(market);
            log.info("创建市场 skill 待审核条目 userId={}, bizNo={}", userId, bizNo);
        }
    }

    private BigDecimal resolvePricePerUse() {
        return DEFAULT_PRICE_PER_USE;
    }

    @Override
    public void incrementUseCount(Long userId, String skillName) {
        if (userId == null || skillName == null || skillName.isBlank()) {
            return;
        }
        UserSkill skill = userSkillMapper.selectOne(
                new LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getSkillName, skillName)
                        .eq(UserSkill::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (skill == null) {
            log.warn("增加风格使用次数失败，风格不存在 userId={} skillName={}", userId, skillName);
            return;
        }
        userSkillMapper.update(null, new UpdateWrapper<UserSkill>()
                .eq("id", skill.getId())
                .setSql("use_count = use_count + 1")
                .set("updated_at", LocalDateTime.now()));
        log.info("增加风格使用次数 userId={} skillName={}", userId, skillName);
    }

    /**
     * 按业务编号获取当前用户的风格；不存在或无权限时抛异常。
     */
    private UserSkill getOwnedSkill(String bizNo, Long userId) {
        LambdaQueryWrapper<UserSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkill::getBizNo, bizNo)
                .eq(UserSkill::getUserId, userId);
        UserSkill skill = userSkillMapper.selectOne(wrapper);
        if (skill == null) {
            throw new BusinessException(SkillErrorCode.SKILL_NOT_FOUND);
        }
        return skill;
    }

    /**
     * 校验当前用户的「我的风格」数量是否达到套餐上限。
     * skill_custom 已改为 quota 类型，值表示可同时保存的自定义风格数量上限。
     */
    private void ensureSkillQuotaNotExceeded(Long userId) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        if (membership == null || membership.getExpiresAt().isBefore(LocalDate.now())) {
            throw new BusinessException(SkillErrorCode.SKILL_QUOTA_EXCEEDED);
        }

        String planKey = membership.getLevel();
        PlanBenefit planBenefit = planBenefitMapper.selectOne(new LambdaQueryWrapper<PlanBenefit>()
                .eq(PlanBenefit::getPlanKey, planKey)
                .eq(PlanBenefit::getBenefitCode, BENEFIT_CODE_STYLE_CUSTOM));
        int limit = planBenefit == null ? 0 : parseInt(planBenefit.getBenefitValue(), 0);
        if (limit <= 0) {
            throw new BusinessException(SkillErrorCode.SKILL_QUOTA_EXCEEDED);
        }

        Long currentCount = userSkillMapper.selectCount(new LambdaQueryWrapper<UserSkill>()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSourceType, SOURCE_TYPE_CUSTOM));
        if (currentCount != null && currentCount >= limit) {
            throw new BusinessException(SkillErrorCode.SKILL_QUOTA_EXCEEDED);
        }
    }

    private int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * 校验提示词名称全局唯一：任意用户的「我的提示词」和「学习的提示词」中均不能存在同名。
     * 系统预设(source_type=3)也存储在同一张表，因此同样受该唯一索引约束。
     *
     * @param skillName 要校验的名称
     * @param excludeId 更新时排除的提示词主键（创建时传 null）
     */
    private void ensureNameUniqueGlobally(String skillName, Long excludeId) {
        LambdaQueryWrapper<UserSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkill::getSkillName, skillName);
        if (excludeId != null) {
            wrapper.ne(UserSkill::getId, excludeId);
        }
        UserSkill existing = userSkillMapper.selectOne(wrapper.last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(SkillErrorCode.SKILL_NAME_EXISTS);
        }
    }

    /**
     * 规范化适用范围字符串：按中英文逗号分割，去重去空，再拼接。
     */
    private String normalizeScope(String scope) {
        if (scope == null) {
            return null;
        }
        String joined = Arrays.stream(scope.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    /**
     * 校验适用范围标签：最多 3 个，每个最多 8 字符。
     */
    private void validateScope(String scope) {
        if (scope == null) {
            return;
        }
        String[] tags = scope.split(",");
        if (tags.length > MAX_SCOPE_TAGS) {
            throw new BusinessException(SkillErrorCode.SKILL_SCOPE_TOO_LONG);
        }
        for (String tag : tags) {
            if (tag.length() > MAX_SCOPE_TAG_LENGTH) {
                throw new BusinessException(SkillErrorCode.SKILL_SCOPE_TOO_LONG);
            }
        }
    }

    private String generateBizNo() {
        return "S" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private UserSkillVO toVO(UserSkill skill) {
        if (skill == null) {
            return null;
        }
        UserSkillVO vo = new UserSkillVO();
        vo.setBizNo(skill.getBizNo());
        vo.setSkillName(skill.getSkillName());
        vo.setPrompt(skill.getPrompt());
        vo.setExcerpt1(skill.getExcerpt1());
        vo.setExcerpt2(skill.getExcerpt2());
        vo.setDescription(skill.getDescription());
        vo.setPromptSummary(skill.getPromptSummary());
        vo.setScope(skill.getScope());
        vo.setEnableStatus(skill.getEnableStatus());
        vo.setSourceType(skill.getSourceType());
        vo.setUseCount(skill.getUseCount());
        vo.setCreatedAt(skill.getCreatedAt());
        vo.setUpdatedAt(skill.getUpdatedAt());
        vo.setAuditStatus(skill.getAuditStatus());
        return vo;
    }
}
