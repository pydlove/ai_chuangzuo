package com.aichuangzuo.admin.modules.style.preset.service.impl;

import com.aichuangzuo.admin.modules.style.entity.UserStyleAggregate;
import com.aichuangzuo.admin.modules.style.preset.dto.SystemStyleRow;
import com.aichuangzuo.admin.modules.style.preset.dto.request.CreateGlobalStyleRequest;
import com.aichuangzuo.admin.modules.style.preset.dto.request.GlobalStylePageRequest;
import com.aichuangzuo.admin.modules.style.preset.dto.request.UpdateGlobalStyleRequest;
import com.aichuangzuo.admin.modules.style.preset.enums.AdminGlobalStyleErrorCode;
import com.aichuangzuo.admin.modules.style.preset.mapper.GlobalStyleAggregateMapper;
import com.aichuangzuo.admin.modules.style.preset.mapper.GlobalStyleMapper;
import com.aichuangzuo.admin.modules.style.preset.service.GlobalStyleService;
import com.aichuangzuo.admin.modules.style.preset.vo.GlobalStyleVO;
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
 * <p>所有写操作落到 {@code u_user_style} 表的 {@code source_type=3} 行；user_id 固定为 0（系统账号）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalStyleServiceImpl implements GlobalStyleService {

    private static final int SOURCE_TYPE_SYSTEM = 3;
    private static final long SYSTEM_USER_ID = 0L;
    private static final int AUDIT_STATUS_APPROVED = 1;

    private final GlobalStyleMapper globalStyleMapper;
    private final GlobalStyleAggregateMapper aggregateMapper;

    @Override
    public IPage<GlobalStyleVO> page(GlobalStylePageRequest request) {
        long offset = (long) (request.getPageNum() - 1) * request.getPageSize();
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;

        List<SystemStyleRow> rows = aggregateMapper.selectGlobalStylePage(
                request.getEnableStatus(), keyword, offset, request.getPageSize());
        long total = aggregateMapper.countGlobalStylePage(request.getEnableStatus(), keyword);

        List<GlobalStyleVO> records = rows.stream().map(this::toVo).toList();

        Page<GlobalStyleVO> page = new Page<>(request.getPageNum(), request.getPageSize());
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CreateGlobalStyleRequest request) {
        validateName(request.getStyleName());

        UserStyleAggregate style = new UserStyleAggregate();
        style.setBizNo(generateBizNo());
        style.setUserId(SYSTEM_USER_ID);
        style.setStyleName(request.getStyleName().trim());
        style.setDescription(StringUtils.trimWhitespace(request.getDescription()));
        style.setPromptSummary(StringUtils.trimWhitespace(request.getPromptSummary()));
        style.setPrompt(request.getPrompt().trim());
        style.setScope(normalizeScope(request.getScope()));
        style.setSourceType(SOURCE_TYPE_SYSTEM);
        style.setAuditStatus(AUDIT_STATUS_APPROVED);
        style.setEnableStatus(1);
        style.setUseCount(0);
        style.setIsDeleted(0);

        globalStyleMapper.insert(style);
        log.info("创建预设风格 bizNo={}, name={}", style.getBizNo(), style.getStyleName());
        return style.getBizNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String bizNo, UpdateGlobalStyleRequest request) {
        if (request.getEnableStatus() == null
                || (request.getEnableStatus() != 0 && request.getEnableStatus() != 1)) {
            throw new BusinessException(AdminGlobalStyleErrorCode.ENABLE_STATUS_INVALID);
        }

        UserStyleAggregate style = loadByBizNo(bizNo);

        String newName = request.getStyleName().trim();
        if (!StringUtils.hasText(newName)) {
            throw new BusinessException(AdminGlobalStyleErrorCode.GLOBAL_STYLE_NAME_EXISTS);
        }
        if (!newName.equals(style.getStyleName())) {
            ensureNameNotExists(newName, bizNo);
        }

        style.setStyleName(newName);
        style.setDescription(StringUtils.trimWhitespace(request.getDescription()));
        style.setPromptSummary(StringUtils.trimWhitespace(request.getPromptSummary()));
        style.setPrompt(request.getPrompt().trim());
        style.setScope(normalizeScope(request.getScope()));
        style.setEnableStatus(request.getEnableStatus());

        globalStyleMapper.updateById(style);
        log.info("更新预设风格 bizNo={}, name={}, enableStatus={}",
                bizNo, newName, request.getEnableStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String bizNo) {
        UserStyleAggregate style = loadByBizNo(bizNo);
        style.setIsDeleted(1);
        globalStyleMapper.updateById(style);
        log.info("软删预设风格 bizNo={}", bizNo);
    }

    // -------- helpers --------

    private UserStyleAggregate loadByBizNo(String bizNo) {
        LambdaQueryWrapper<UserStyleAggregate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStyleAggregate::getBizNo, bizNo)
                .eq(UserStyleAggregate::getSourceType, SOURCE_TYPE_SYSTEM)
                .eq(UserStyleAggregate::getIsDeleted, 0);
        UserStyleAggregate style = globalStyleMapper.selectOne(wrapper);
        if (style == null) {
            throw new BusinessException(AdminGlobalStyleErrorCode.GLOBAL_STYLE_NOT_FOUND);
        }
        return style;
    }

    private void validateName(String name) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(name.trim())) {
            throw new BusinessException(AdminGlobalStyleErrorCode.GLOBAL_STYLE_NAME_EXISTS);
        }
        ensureNameNotExists(name.trim(), null);
    }

    private void ensureNameNotExists(String name, String excludeBizNo) {
        LambdaQueryWrapper<UserStyleAggregate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStyleAggregate::getUserId, SYSTEM_USER_ID)
                .eq(UserStyleAggregate::getSourceType, SOURCE_TYPE_SYSTEM)
                .eq(UserStyleAggregate::getStyleName, name)
                .eq(UserStyleAggregate::getIsDeleted, 0);
        if (excludeBizNo != null) {
            wrapper.ne(UserStyleAggregate::getBizNo, excludeBizNo);
        }
        Long count = globalStyleMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(AdminGlobalStyleErrorCode.GLOBAL_STYLE_NAME_EXISTS);
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

    private GlobalStyleVO toVo(SystemStyleRow row) {
        GlobalStyleVO vo = new GlobalStyleVO();
        vo.setId(row.getBizNo());
        vo.setName(row.getStyleName());
        vo.setDescription(row.getDescription());
        vo.setPromptSummary(row.getPromptSummary());
        vo.setPrompt(row.getPrompt());
        vo.setScope(row.getScope());
        vo.setStatus(row.getEnableStatus() != null && row.getEnableStatus() == 1 ? "enabled" : "disabled");
        vo.setCreatorName("系统");
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }
}