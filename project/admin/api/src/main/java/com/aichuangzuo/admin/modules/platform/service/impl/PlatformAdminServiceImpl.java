package com.aichuangzuo.admin.modules.platform.service.impl;

import com.aichuangzuo.admin.modules.platform.dto.request.PlatformSaveRequest;
import com.aichuangzuo.admin.modules.platform.mapper.PlatformAdminMapper;
import com.aichuangzuo.admin.modules.platform.service.PlatformAdminService;
import com.aichuangzuo.admin.modules.platform.vo.PlatformVO;
import com.aichuangzuo.shared.entity.Platform;
import com.aichuangzuo.shared.enums.error.SystemErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自媒体平台配置管理端服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAdminServiceImpl implements PlatformAdminService {

    private final PlatformAdminMapper platformAdminMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<PlatformVO> list() {
        return platformAdminMapper.selectList(new LambdaQueryWrapper<Platform>()
                        .orderByAsc(Platform::getSortOrder)
                        .orderByAsc(Platform::getId))
                .stream()
                .map(p -> PlatformVO.from(p, objectMapper))
                .collect(Collectors.toList());
    }

    @Override
    public PlatformVO save(PlatformSaveRequest request) {
        Platform existing = platformAdminMapper.selectOne(new LambdaQueryWrapper<Platform>()
                .eq(Platform::getPlatformKey, request.getPlatformKey()));
        if (existing != null) {
            throw new BusinessException(SystemErrorCode.PARAM_VALIDATION_ERROR.getCode(), "平台 key 已存在");
        }
        clearDefaultIfNeeded(request.getIsDefault());
        Platform entity = toEntity(request);
        platformAdminMapper.insert(entity);
        log.info("新增自媒体平台 platformKey={}", entity.getPlatformKey());
        return PlatformVO.from(entity, objectMapper);
    }

    @Override
    public PlatformVO update(Long id, PlatformSaveRequest request) {
        Platform entity = platformAdminMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!entity.getPlatformKey().equals(request.getPlatformKey())) {
            Platform conflict = platformAdminMapper.selectOne(new LambdaQueryWrapper<Platform>()
                    .eq(Platform::getPlatformKey, request.getPlatformKey()));
            if (conflict != null && !conflict.getId().equals(id)) {
                throw new BusinessException(SystemErrorCode.PARAM_VALIDATION_ERROR.getCode(), "平台 key 已存在");
            }
        }
        clearDefaultIfNeeded(request.getIsDefault());
        entity.setPlatformKey(request.getPlatformKey());
        entity.setPlatformName(request.getPlatformName());
        entity.setDescription(request.getDescription());
        entity.setRecommendWords(request.getRecommendWords());
        entity.setTrait(request.getTrait());
        entity.setWordCountPresetsJson(toJson(request.getWordCountPresets()));
        entity.setTagline(request.getTagline());
        entity.setContentFormJson(toJson(request.getContentForm()));
        entity.setMonetizationJson(toJson(request.getMonetization()));
        entity.setThreshold(request.getThreshold());
        entity.setBestFor(request.getBestFor());
        entity.setReason(request.getReason());
        entity.setMonetizationEase(request.getMonetizationEase());
        entity.setTimeToIncome(request.getTimeToIncome());
        entity.setIncomeRange(request.getIncomeRange());
        entity.setDifficulty(request.getDifficulty());
        entity.setSortOrder(request.getSortOrder());
        entity.setStatus(request.getStatus());
        entity.setIsDefault(request.getIsDefault());
        entity.setIconUrl(request.getIconUrl());
        platformAdminMapper.updateById(entity);
        log.info("更新自媒体平台 id={}, platformKey={}", id, entity.getPlatformKey());
        return PlatformVO.from(entity, objectMapper);
    }

    @Override
    public void delete(Long id) {
        Platform entity = platformAdminMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND);
        }
        platformAdminMapper.deleteById(id);
        log.info("删除自媒体平台 id={}, platformKey={}", id, entity.getPlatformKey());
    }

    private void clearDefaultIfNeeded(Integer isDefault) {
        if (Integer.valueOf(1).equals(isDefault)) {
            platformAdminMapper.update(null, Wrappers.<Platform>lambdaUpdate()
                    .eq(Platform::getIsDefault, 1)
                    .set(Platform::getIsDefault, 0));
        }
    }

    private Platform toEntity(PlatformSaveRequest request) {
        Platform entity = new Platform();
        entity.setPlatformKey(request.getPlatformKey());
        entity.setPlatformName(request.getPlatformName());
        entity.setDescription(request.getDescription());
        entity.setRecommendWords(request.getRecommendWords());
        entity.setTrait(request.getTrait());
        entity.setWordCountPresetsJson(toJson(request.getWordCountPresets()));
        entity.setTagline(request.getTagline());
        entity.setContentFormJson(toJson(request.getContentForm()));
        entity.setMonetizationJson(toJson(request.getMonetization()));
        entity.setThreshold(request.getThreshold());
        entity.setBestFor(request.getBestFor());
        entity.setReason(request.getReason());
        entity.setMonetizationEase(request.getMonetizationEase());
        entity.setTimeToIncome(request.getTimeToIncome());
        entity.setIncomeRange(request.getIncomeRange());
        entity.setDifficulty(request.getDifficulty());
        entity.setSortOrder(request.getSortOrder());
        entity.setStatus(request.getStatus());
        entity.setIsDefault(request.getIsDefault());
        entity.setIconUrl(request.getIconUrl());
        return entity;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("序列化 JSON 字段失败: " + e.getMessage(), e);
        }
    }
}
