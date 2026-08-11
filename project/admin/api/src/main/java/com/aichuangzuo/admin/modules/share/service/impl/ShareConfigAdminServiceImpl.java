package com.aichuangzuo.admin.modules.share.service.impl;

import com.aichuangzuo.admin.modules.share.dto.request.ShareConfigQueryRequest;
import com.aichuangzuo.admin.modules.share.dto.request.ShareConfigSaveRequest;
import com.aichuangzuo.admin.modules.share.enums.AdminShareErrorCode;
import com.aichuangzuo.admin.modules.share.mapper.ShareConfigMapper;
import com.aichuangzuo.admin.modules.share.service.ShareConfigAdminService;
import com.aichuangzuo.admin.modules.share.vo.ShareConfigAdminVO;
import com.aichuangzuo.shared.entity.ShareConfig;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareConfigAdminServiceImpl implements ShareConfigAdminService {

    private final ShareConfigMapper shareConfigMapper;

    @Override
    public PageResult list(ShareConfigQueryRequest request) {
        QueryWrapper<ShareConfig> wrapper = new QueryWrapper<ShareConfig>()
                .eq("is_deleted", 0)
                .orderByAsc("sort_order")
                .orderByDesc("updated_at");

        if (StringUtils.isNotBlank(request.getSceneKey())) {
            wrapper.eq("scene_key", request.getSceneKey());
        }
        if (request.getEnabled() != null) {
            wrapper.eq("enabled", request.getEnabled());
        }

        Page<ShareConfig> page = new Page<>(request.getPage(), request.getSize());
        Page<ShareConfig> result = shareConfigMapper.selectPage(page, wrapper);

        List<ShareConfigAdminVO> items = result.getRecords().stream()
                .map(this::toAdminVO)
                .collect(Collectors.toList());
        return new PageResult(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ShareConfigAdminVO get(Long id) {
        ShareConfig config = shareConfigMapper.selectById(id);
        if (config == null || config.getIsDeleted() != null && config.getIsDeleted() == 1) {
            throw new BusinessException(AdminShareErrorCode.CONFIG_NOT_FOUND);
        }
        return toAdminVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareConfig create(ShareConfigSaveRequest request, Long adminId) {
        validateSceneKeyUnique(request.getSceneKey(), null);

        ShareConfig config = new ShareConfig();
        config.setSceneKey(request.getSceneKey());
        config.setTitle(request.getTitle());
        config.setContent(request.getContent());
        config.setEnabled(request.getEnabled());
        config.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        config.setCreatedBy(adminId == null ? 0L : adminId);
        config.setUpdatedBy(adminId == null ? 0L : adminId);
        shareConfigMapper.insert(config);
        log.info("管理员创建分享配置, adminId={}, sceneKey={}", adminId, config.getSceneKey());
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareConfig update(Long id, ShareConfigSaveRequest request, Long adminId) {
        ShareConfig existing = shareConfigMapper.selectById(id);
        if (existing == null || existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
            throw new BusinessException(AdminShareErrorCode.CONFIG_NOT_FOUND);
        }

        validateSceneKeyUnique(request.getSceneKey(), id);

        existing.setSceneKey(request.getSceneKey());
        existing.setTitle(request.getTitle());
        existing.setContent(request.getContent());
        existing.setEnabled(request.getEnabled());
        existing.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        existing.setUpdatedBy(adminId == null ? 0L : adminId);
        shareConfigMapper.updateById(existing);
        log.info("管理员更新分享配置, adminId={}, configId={}, sceneKey={}", adminId, id, existing.getSceneKey());
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long adminId) {
        ShareConfig existing = shareConfigMapper.selectById(id);
        if (existing == null || existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
            throw new BusinessException(AdminShareErrorCode.CONFIG_NOT_FOUND);
        }
        shareConfigMapper.deleteById(id);
        log.info("管理员删除分享配置, adminId={}, configId={}, sceneKey={}", adminId, id, existing.getSceneKey());
    }

    private void validateSceneKeyUnique(String sceneKey, Long excludeId) {
        QueryWrapper<ShareConfig> wrapper = new QueryWrapper<ShareConfig>()
                .eq("scene_key", sceneKey)
                .eq("is_deleted", 0);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        Long count = shareConfigMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(AdminShareErrorCode.SCENE_KEY_EXISTS);
        }
    }

    private ShareConfigAdminVO toAdminVO(ShareConfig config) {
        ShareConfigAdminVO vo = new ShareConfigAdminVO();
        vo.setId(config.getId());
        vo.setSceneKey(config.getSceneKey());
        vo.setTitle(config.getTitle());
        vo.setContent(config.getContent());
        vo.setEnabled(config.getEnabled());
        vo.setSortOrder(config.getSortOrder());
        vo.setCreatedAt(config.getCreatedAt());
        vo.setUpdatedAt(config.getUpdatedAt());
        return vo;
    }
}
