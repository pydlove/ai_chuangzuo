package com.aichuangzuo.user.modules.skill.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.benefit.entity.PlanBenefit;
import com.aichuangzuo.user.modules.benefit.mapper.PlanBenefitMapper;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.skill.dto.request.CreateSkillRequest;
import com.aichuangzuo.user.modules.skill.dto.request.UpdateSkillRequest;
import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.skill.enums.SkillErrorCode;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.service.UserSkillService;
import com.aichuangzuo.user.modules.skill.vo.UserSkillVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    private static final String BENEFIT_CODE_STYLE_CUSTOM = "skill_custom";

    private final UserSkillMapper userSkillMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final PlanBenefitMapper planBenefitMapper;

    @Override
    public List<UserSkillVO> listMySkills(Integer sourceType) {
        Long userId = SecurityUserContext.getCurrentUserId();
        LambdaQueryWrapper<UserSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSourceType, sourceType == null ? SOURCE_TYPE_CUSTOM : sourceType)
                .orderByDesc(UserSkill::getUpdatedAt);
        return userSkillMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public UserSkillVO createSkill(CreateSkillRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        String skillName = request.getSkillName().trim();
        String prompt = request.getPrompt().trim();
        String scope = normalizeScope(request.getScope());

        validateScope(scope);
        ensureNameNotExists(userId, skillName, null);
        ensureSkillQuotaNotExceeded(userId);

        UserSkill skill = new UserSkill();
        skill.setBizNo(generateBizNo());
        skill.setUserId(userId);
        skill.setSkillName(skillName);
        skill.setPrompt(prompt);
        skill.setScope(scope);
        skill.setSourceType(request.getSourceType() == null ? SOURCE_TYPE_CUSTOM : request.getSourceType());
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

        validateScope(scope);
        ensureNameNotExists(userId, skillName, skill.getId());

        skill.setSkillName(skillName);
        skill.setPrompt(prompt);
        skill.setScope(scope);
        // 修改后重新进入待审核状态，并清空上一次的打回原因
        skill.setAuditStatus(0);
        skill.setRejectReason(null);

        userSkillMapper.updateById(skill);
        log.info("更新风格成功 userId={}, bizNo={}, skillName={}", userId, bizNo, skillName);
        return toVO(skill);
    }

    @Override
    public void deleteSkill(String bizNo) {
        Long userId = SecurityUserContext.getCurrentUserId();
        UserSkill skill = getOwnedSkill(bizNo, userId);
        userSkillMapper.deleteById(skill.getId());
        log.info("删除风格成功 userId={}, bizNo={}", userId, bizNo);
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
     * 校验同一用户下风格名是否重复。
     *
     * @param excludeId 排除的风格主键（更新时使用）
     */
    private void ensureNameNotExists(Long userId, String skillName, Long excludeId) {
        LambdaQueryWrapper<UserSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillName, skillName);
        if (excludeId != null) {
            wrapper.ne(UserSkill::getId, excludeId);
        }
        Long count = userSkillMapper.selectCount(wrapper);
        if (count != null && count > 0) {
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
