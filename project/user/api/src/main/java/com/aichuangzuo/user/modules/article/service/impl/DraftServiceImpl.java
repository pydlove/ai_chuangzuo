package com.aichuangzuo.user.modules.article.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.article.dto.request.SaveDraftRequest;
import com.aichuangzuo.user.modules.article.dto.request.UpdateDraftRequest;
import com.aichuangzuo.user.modules.article.entity.Draft;
import com.aichuangzuo.user.modules.article.enums.ArticleErrorCode;
import com.aichuangzuo.user.modules.article.mapper.DraftMapper;
import com.aichuangzuo.user.modules.article.service.DraftService;
import com.aichuangzuo.user.modules.article.vo.DraftPageVO;
import com.aichuangzuo.user.modules.article.vo.DraftVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户草稿服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DraftServiceImpl implements DraftService {

    private final DraftMapper draftMapper;

    @Override
    public DraftPageVO list(Long userId, String keyword, long page, long pageSize) {
        LambdaQueryWrapper<Draft> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Draft::getUserId, userId)
                .eq(Draft::getIsDeleted, 0)
                .orderByDesc(Draft::getSavedAt)
                .orderByDesc(Draft::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Draft::getCustomTitle, keyword.trim())
                    .or().like(Draft::getCustomRequirement, keyword.trim()));
        }
        IPage<Draft> result = draftMapper.selectPage(new Page<>(page, pageSize), wrapper);
        DraftPageVO vo = new DraftPageVO();
        vo.setList(result.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        vo.setTotal(result.getTotal());
        vo.setPage(result.getCurrent());
        vo.setPageSize(result.getSize());
        return vo;
    }

    @Override
    public DraftVO get(Long userId, String bizNo) {
        Draft draft = mustFind(userId, bizNo);
        return toVo(draft);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String save(Long userId, SaveDraftRequest request) {
        Draft draft = new Draft();
        draft.setBizNo(generateBizNo());
        draft.setUserId(userId);
        draft.setCustomTitle(request.getCustomTitle());
        draft.setCustomRequirement(request.getCustomRequirement());
        draft.setPlatform(request.getPlatform());
        draft.setWordCount(request.getWordCount() == null ? 0 : Math.max(0, request.getWordCount()));
        draft.setStyle(request.getStyle());
        draft.setTemplate(request.getTemplate());
        draft.setSavedAt(LocalDateTime.now());
        draftMapper.insert(draft);
        log.info("保存草稿完成 userId={}, bizNo={}", userId, draft.getBizNo());
        return draft.getBizNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, String bizNo, UpdateDraftRequest request) {
        mustFind(userId, bizNo);
        LambdaUpdateWrapper<Draft> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Draft::getUserId, userId)
                .eq(Draft::getBizNo, bizNo)
                .eq(Draft::getIsDeleted, 0);
        boolean touched = false;
        if (request.getCustomTitle() != null) {
            wrapper.set(Draft::getCustomTitle, request.getCustomTitle());
            touched = true;
        }
        if (request.getCustomRequirement() != null) {
            wrapper.set(Draft::getCustomRequirement, request.getCustomRequirement());
            touched = true;
        }
        if (request.getPlatform() != null) {
            wrapper.set(Draft::getPlatform, request.getPlatform());
            touched = true;
        }
        if (request.getWordCount() != null) {
            wrapper.set(Draft::getWordCount, Math.max(0, request.getWordCount()));
            touched = true;
        }
        if (request.getStyle() != null) {
            wrapper.set(Draft::getStyle, request.getStyle());
            touched = true;
        }
        if (request.getTemplate() != null) {
            wrapper.set(Draft::getTemplate, request.getTemplate());
            touched = true;
        }
        // 每次 update 都把 savedAt 滚到当前，便于草稿箱按保存时间排序
        wrapper.set(Draft::getSavedAt, request.getSavedAt() != null ? request.getSavedAt() : LocalDateTime.now());
        touched = true;
        if (!touched) {
            return;
        }
        draftMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, String bizNo) {
        mustFind(userId, bizNo);
        draftMapper.update(null, new LambdaUpdateWrapper<Draft>()
                .eq(Draft::getUserId, userId)
                .eq(Draft::getBizNo, bizNo)
                .eq(Draft::getIsDeleted, 0)
                .set(Draft::getIsDeleted, 1));
    }

    private Draft mustFind(Long userId, String bizNo) {
        Draft draft = draftMapper.selectOne(new LambdaQueryWrapper<Draft>()
                .eq(Draft::getUserId, userId)
                .eq(Draft::getBizNo, bizNo)
                .eq(Draft::getIsDeleted, 0));
        if (draft == null) {
            throw new BusinessException(ArticleErrorCode.DRAFT_NOT_FOUND);
        }
        return draft;
    }

    private DraftVO toVo(Draft draft) {
        DraftVO vo = new DraftVO();
        vo.setBizNo(draft.getBizNo());
        vo.setCustomTitle(draft.getCustomTitle());
        vo.setCustomRequirement(draft.getCustomRequirement());
        vo.setPlatform(draft.getPlatform());
        vo.setWordCount(draft.getWordCount());
        vo.setStyle(draft.getStyle());
        vo.setTemplate(draft.getTemplate());
        vo.setSavedAt(draft.getSavedAt());
        vo.setCreatedAt(draft.getCreatedAt());
        vo.setUpdatedAt(draft.getUpdatedAt());
        return vo;
    }

    private String generateBizNo() {
        return "D" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}