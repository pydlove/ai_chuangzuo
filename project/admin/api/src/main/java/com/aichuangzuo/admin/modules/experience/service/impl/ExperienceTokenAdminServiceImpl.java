package com.aichuangzuo.admin.modules.experience.service.impl;

import com.aichuangzuo.admin.modules.experience.dto.request.ExperienceTokenBatchGenerateRequest;
import com.aichuangzuo.admin.modules.experience.dto.request.ExperienceTokenQueryRequest;
import com.aichuangzuo.admin.modules.experience.mapper.ExperienceTokenMapper;
import com.aichuangzuo.admin.modules.experience.service.ExperienceTokenAdminService;
import com.aichuangzuo.admin.modules.experience.util.ExperienceTokenGenerator;
import com.aichuangzuo.admin.modules.experience.vo.ExperienceTokenAdminVO;
import com.aichuangzuo.shared.entity.ExperienceToken;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceTokenAdminServiceImpl implements ExperienceTokenAdminService {

    private final ExperienceTokenMapper experienceTokenMapper;
    private final ExperienceTokenGenerator tokenGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> batchGenerate(ExperienceTokenBatchGenerateRequest request, Long adminId) {
        String batchId = tokenGenerator.generateBatchId();
        int count = request.getCount();

        List<String> tokens = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ExperienceToken token = new ExperienceToken();
            token.setBatchId(batchId);
            token.setToken(tokenGenerator.generateToken());
            token.setPlanKey(request.getPlanKey());
            token.setMembershipDays(request.getMembershipDays());
            token.setStatus(0);
            token.setExpiresAt(request.getExpiresAt());
            token.setTenantId(0L);
            token.setCreatedBy(adminId == null ? 0L : adminId);
            token.setUpdatedBy(adminId == null ? 0L : adminId);
            experienceTokenMapper.insert(token);
            tokens.add(token.getToken());
        }

        log.info("管理员批量生成体验令牌, adminId={}, batchId={}, count={}", adminId, batchId, count);
        return tokens;
    }

    @Override
    public PageResult list(ExperienceTokenQueryRequest request) {
        QueryWrapper<ExperienceToken> wrapper = new QueryWrapper<ExperienceToken>()
                .eq("is_deleted", 0)
                .orderByDesc("created_at");

        if (request.getBatchId() != null && !request.getBatchId().isBlank()) {
            wrapper.eq("batch_id", request.getBatchId());
        }
        if (request.getStatus() != null) {
            wrapper.eq("status", request.getStatus());
        }

        Page<ExperienceToken> page = new Page<>(request.getPage(), request.getSize());
        Page<ExperienceToken> result = experienceTokenMapper.selectPage(page, wrapper);

        List<ExperienceTokenAdminVO> items = result.getRecords().stream()
                .map(this::toAdminVO)
                .collect(Collectors.toList());
        return new PageResult(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private ExperienceTokenAdminVO toAdminVO(ExperienceToken token) {
        ExperienceTokenAdminVO vo = new ExperienceTokenAdminVO();
        vo.setId(token.getId());
        vo.setBatchId(token.getBatchId());
        vo.setToken(token.getToken());
        vo.setPlanKey(token.getPlanKey());
        vo.setMembershipDays(token.getMembershipDays());
        vo.setStatus(token.getStatus());
        vo.setUsedByUserId(token.getUsedByUserId());
        vo.setUsedAt(token.getUsedAt());
        vo.setExpiresAt(token.getExpiresAt());
        vo.setCreatedAt(token.getCreatedAt());
        vo.setUpdatedAt(token.getUpdatedAt());
        return vo;
    }
}
