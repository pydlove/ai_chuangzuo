package com.aichuangzuo.admin.modules.skill.preset.service.impl;

import com.aichuangzuo.admin.modules.skill.entity.UserSkillAggregate;
import com.aichuangzuo.admin.modules.skill.preset.dto.SystemSkillRow;
import com.aichuangzuo.admin.modules.skill.preset.dto.request.CreateGlobalSkillRequest;
import com.aichuangzuo.admin.modules.skill.preset.dto.request.GlobalSkillPageRequest;
import com.aichuangzuo.admin.modules.skill.preset.dto.request.UpdateGlobalSkillRequest;
import com.aichuangzuo.admin.modules.skill.preset.enums.AdminGlobalSkillErrorCode;
import com.aichuangzuo.admin.modules.skill.preset.mapper.GlobalSkillAggregateMapper;
import com.aichuangzuo.admin.modules.skill.preset.mapper.GlobalSkillMapper;
import com.aichuangzuo.admin.modules.skill.preset.service.GlobalSkillService;
import com.aichuangzuo.admin.modules.skill.preset.vo.GlobalSkillVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * 预设风格服务实现。
 *
 * <p>所有写操作落到 {@code u_user_skill} 表的 {@code source_type=3} 行；user_id 固定为 0（系统账号）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalSkillServiceImpl implements GlobalSkillService {

    private static final int SOURCE_TYPE_SYSTEM = 3;
    private static final long SYSTEM_USER_ID = 0L;
    private static final int AUDIT_STATUS_APPROVED = 1;

    private final GlobalSkillMapper globalSkillMapper;
    private final GlobalSkillAggregateMapper aggregateMapper;

    @Override
    public IPage<GlobalSkillVO> page(GlobalSkillPageRequest request) {
        long offset = (long) (request.getPageNum() - 1) * request.getPageSize();
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;

        List<SystemSkillRow> rows = aggregateMapper.selectGlobalSkillPage(
                request.getEnableStatus(), keyword, offset, request.getPageSize());
        long total = aggregateMapper.countGlobalSkillPage(request.getEnableStatus(), keyword);

        List<GlobalSkillVO> records = rows.stream().map(this::toVo).toList();

        Page<GlobalSkillVO> page = new Page<>(request.getPageNum(), request.getPageSize());
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CreateGlobalSkillRequest request) {
        validateName(request.getSkillName());

        UserSkillAggregate skill = new UserSkillAggregate();
        skill.setBizNo(generateBizNo());
        skill.setUserId(SYSTEM_USER_ID);
        skill.setSkillName(request.getSkillName().trim());
        skill.setDescription(StringUtils.trimWhitespace(request.getDescription()));
        skill.setPromptSummary(StringUtils.trimWhitespace(request.getPromptSummary()));
        skill.setPrompt(request.getPrompt().trim());
        skill.setPromptExtra(StringUtils.trimWhitespace(request.getPromptExtra()));
        skill.setScope(normalizeScope(request.getScope()));
        skill.setSourceType(SOURCE_TYPE_SYSTEM);
        skill.setAuditStatus(AUDIT_STATUS_APPROVED);
        skill.setEnableStatus(1);
        skill.setUseCount(0);
        skill.setIsDeleted(0);

        globalSkillMapper.insert(skill);
        log.info("创建预设风格 bizNo={}, name={}", skill.getBizNo(), skill.getSkillName());
        return skill.getBizNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String bizNo, UpdateGlobalSkillRequest request) {
        if (request.getEnableStatus() == null
                || (request.getEnableStatus() != 0 && request.getEnableStatus() != 1)) {
            throw new BusinessException(AdminGlobalSkillErrorCode.ENABLE_STATUS_INVALID);
        }

        UserSkillAggregate skill = loadByBizNo(bizNo);

        String newName = request.getSkillName().trim();
        if (!StringUtils.hasText(newName)) {
            throw new BusinessException(AdminGlobalSkillErrorCode.GLOBAL_SKILL_NAME_EXISTS);
        }
        if (!newName.equals(skill.getSkillName())) {
            ensureNameNotExists(newName, bizNo);
        }

        skill.setSkillName(newName);
        skill.setDescription(StringUtils.trimWhitespace(request.getDescription()));
        skill.setPromptSummary(StringUtils.trimWhitespace(request.getPromptSummary()));
        skill.setPrompt(request.getPrompt().trim());
        skill.setPromptExtra(StringUtils.trimWhitespace(request.getPromptExtra()));
        skill.setScope(normalizeScope(request.getScope()));
        skill.setEnableStatus(request.getEnableStatus());

        globalSkillMapper.updateById(skill);
        log.info("更新预设风格 bizNo={}, name={}, enableStatus={}",
                bizNo, newName, request.getEnableStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String bizNo) {
        UserSkillAggregate skill = loadByBizNo(bizNo);
        globalSkillMapper.deleteByIdPhysical(skill.getId());
        log.info("物理删除预设风格 bizNo={}", bizNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(List<String> bizNos) {
        int count = 0;
        for (String bizNo : bizNos) {
            UserSkillAggregate skill = loadByBizNo(bizNo);
            globalSkillMapper.deleteByIdPhysical(skill.getId());
            count++;
        }
        log.info("批量物理删除预设风格完成, count={}, bizNos={}", count, bizNos);
        return count;
    }

    // -------- helpers --------

    private UserSkillAggregate loadByBizNo(String bizNo) {
        LambdaQueryWrapper<UserSkillAggregate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkillAggregate::getBizNo, bizNo)
                .eq(UserSkillAggregate::getSourceType, SOURCE_TYPE_SYSTEM)
                .eq(UserSkillAggregate::getIsDeleted, 0);
        UserSkillAggregate skill = globalSkillMapper.selectOne(wrapper);
        if (skill == null) {
            throw new BusinessException(AdminGlobalSkillErrorCode.GLOBAL_SKILL_NOT_FOUND);
        }
        return skill;
    }

    private void validateName(String name) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(name.trim())) {
            throw new BusinessException(AdminGlobalSkillErrorCode.GLOBAL_SKILL_NAME_EXISTS);
        }
        ensureNameNotExists(name.trim(), null);
    }

    private void ensureNameNotExists(String name, String excludeBizNo) {
        LambdaQueryWrapper<UserSkillAggregate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkillAggregate::getUserId, SYSTEM_USER_ID)
                .eq(UserSkillAggregate::getSourceType, SOURCE_TYPE_SYSTEM)
                .eq(UserSkillAggregate::getSkillName, name)
                .eq(UserSkillAggregate::getIsDeleted, 0);
        if (excludeBizNo != null) {
            wrapper.ne(UserSkillAggregate::getBizNo, excludeBizNo);
        }
        Long count = globalSkillMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(AdminGlobalSkillErrorCode.GLOBAL_SKILL_NAME_EXISTS);
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
        return "GS" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private GlobalSkillVO toVo(SystemSkillRow row) {
        GlobalSkillVO vo = new GlobalSkillVO();
        vo.setId(row.getBizNo());
        vo.setName(row.getSkillName());
        vo.setDescription(row.getDescription());
        vo.setPromptSummary(row.getPromptSummary());
        vo.setPrompt(row.getPrompt());
        vo.setPromptExtra(row.getPromptExtra());
        vo.setScope(row.getScope());
        vo.setStatus(row.getEnableStatus() != null && row.getEnableStatus() == 1 ? "enabled" : "disabled");
        vo.setCreatorName("系统");
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }
}