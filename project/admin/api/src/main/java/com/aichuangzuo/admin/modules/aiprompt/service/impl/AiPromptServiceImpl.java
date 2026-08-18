package com.aichuangzuo.admin.modules.aiprompt.service.impl;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptCreateRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptQueryRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptUpdateRequest;
import com.aichuangzuo.admin.modules.aiprompt.mapper.AiPromptMapper;
import com.aichuangzuo.admin.modules.aiprompt.service.AiPromptService;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptDetailVO;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptVO;
import com.aichuangzuo.shared.entity.AiPrompt;
import com.aichuangzuo.shared.enums.error.AdminAiPromptErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptServiceImpl implements AiPromptService {

    private final AiPromptMapper aiPromptMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult list(AiPromptQueryRequest request) {
        long page = Math.max(1, request.getPage() == null ? 1 : request.getPage());
        long pageSize = Math.min(Math.max(1, request.getPageSize() == null ? 20 : request.getPageSize()), 100);
        String keyword = StringUtils.trimToEmpty(request.getKeyword());

        LambdaQueryWrapper<AiPrompt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPrompt::getIsDeleted, 0);
        wrapper.eq(StringUtils.isNotBlank(request.getModule()), AiPrompt::getModule, request.getModule());
        wrapper.eq(StringUtils.isNotBlank(request.getCategory()), AiPrompt::getCategory, request.getCategory());
        wrapper.eq(request.getStatus() != null, AiPrompt::getStatus, request.getStatus());
        wrapper.and(StringUtils.isNotBlank(keyword), w -> w
                .like(AiPrompt::getPromptCode, keyword)
                .or()
                .like(AiPrompt::getPromptName, keyword));
        wrapper.orderByDesc(AiPrompt::getUpdatedAt);

        Page<AiPrompt> pageParam = new Page<>(page, pageSize);
        Page<AiPrompt> result = aiPromptMapper.selectPage(pageParam, wrapper);

        return new PageResult(
                result.getRecords().stream().map(this::toListVo).toList(),
                result.getTotal(),
                page,
                pageSize
        );
    }

    @Override
    public AiPromptDetailVO get(Long id) {
        AiPrompt entity = requireById(id);
        return toDetailVo(entity);
    }

    @Override
    public Long create(AiPromptCreateRequest request) {
        checkCodeUnique(request.getPromptCode(), null);

        AiPrompt entity = new AiPrompt();
        entity.setPromptCode(request.getPromptCode().trim());
        entity.setPromptName(request.getPromptName().trim());
        entity.setModule(request.getModule().trim());
        entity.setCategory(StringUtils.trimToNull(request.getCategory()));
        entity.setSystemRole(StringUtils.trimToNull(request.getSystemRole()));
        entity.setUserPrompt(request.getUserPrompt().trim());
        entity.setVariableSchema(toJson(request.getVariableSchema()));
        entity.setStatus(request.getStatus());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setDescription(StringUtils.trimToNull(request.getDescription()));
        entity.setTenantId(0L);
        entity.setIsDeleted(0);
        entity.setCreatedBy(currentAdminIdOrZero());
        entity.setUpdatedBy(entity.getCreatedBy());

        aiPromptMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, AiPromptUpdateRequest request) {
        AiPrompt entity = requireById(id);

        entity.setPromptName(request.getPromptName().trim());
        entity.setModule(request.getModule().trim());
        entity.setCategory(StringUtils.trimToNull(request.getCategory()));
        entity.setSystemRole(StringUtils.trimToNull(request.getSystemRole()));
        entity.setUserPrompt(request.getUserPrompt().trim());
        entity.setVariableSchema(toJson(request.getVariableSchema()));
        entity.setStatus(request.getStatus());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setDescription(StringUtils.trimToNull(request.getDescription()));
        entity.setUpdatedBy(currentAdminIdOrZero());

        aiPromptMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        AiPrompt entity = requireById(id);
        entity.setIsDeleted(1);
        entity.setUpdatedBy(currentAdminIdOrZero());
        aiPromptMapper.updateById(entity);
    }

    @Override
    public void enable(Long id) {
        updateStatus(id, 1);
    }

    @Override
    public void disable(Long id) {
        updateStatus(id, 0);
    }

    private void updateStatus(Long id, int status) {
        AiPrompt entity = requireById(id);
        entity.setStatus(status);
        entity.setUpdatedBy(currentAdminIdOrZero());
        aiPromptMapper.updateById(entity);
    }

    private AiPrompt requireById(Long id) {
        AiPrompt entity = aiPromptMapper.selectById(id);
        if (entity == null || (entity.getIsDeleted() != null && entity.getIsDeleted() == 1)) {
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_NOT_FOUND);
        }
        return entity;
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<AiPrompt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPrompt::getPromptCode, code.trim());
        wrapper.eq(AiPrompt::getIsDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(AiPrompt::getId, excludeId);
        }
        if (aiPromptMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_CODE_EXISTS);
        }
    }

    private AiPromptVO toListVo(AiPrompt entity) {
        AiPromptVO vo = new AiPromptVO();
        vo.setId(entity.getId());
        vo.setPromptCode(entity.getPromptCode());
        vo.setPromptName(entity.getPromptName());
        vo.setModule(entity.getModule());
        vo.setCategory(entity.getCategory());
        vo.setStatus(entity.getStatus());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private AiPromptDetailVO toDetailVo(AiPrompt entity) {
        AiPromptDetailVO vo = new AiPromptDetailVO();
        vo.setId(entity.getId());
        vo.setPromptCode(entity.getPromptCode());
        vo.setPromptName(entity.getPromptName());
        vo.setModule(entity.getModule());
        vo.setCategory(entity.getCategory());
        vo.setSystemRole(entity.getSystemRole());
        vo.setUserPrompt(entity.getUserPrompt());
        vo.setVariableSchema(parseVariableSchema(entity.getVariableSchema()));
        vo.setStatus(entity.getStatus());
        vo.setSortOrder(entity.getSortOrder());
        vo.setDescription(entity.getDescription());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    @SneakyThrows
    private String toJson(List<AiPromptCreateRequest.AiPromptVariableRequest> variables) {
        if (variables == null) {
            return null;
        }
        return objectMapper.writeValueAsString(variables);
    }

    @SneakyThrows
    private List<AiPromptDetailVO.AiPromptVariableVO> parseVariableSchema(String json) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private Long currentAdminIdOrZero() {
        Long id = SecurityAdminContext.getCurrentAdminUserId();
        return id == null ? 0L : id;
    }
}
