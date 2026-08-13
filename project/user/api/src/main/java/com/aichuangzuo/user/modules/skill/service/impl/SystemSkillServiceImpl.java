package com.aichuangzuo.user.modules.skill.service.impl;

import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.service.SystemSkillService;
import com.aichuangzuo.user.modules.skill.vo.SystemSkillVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统预设风格服务实现。
 *
 * <p>只读视图：从 {@code u_user_skill} 过滤 {@code source_type=3 AND enable_status=1 AND is_deleted=0}。
 * 不维护：增删改由管理端负责。
 */
@Service
@RequiredArgsConstructor
public class SystemSkillServiceImpl implements SystemSkillService {

    private static final int SOURCE_TYPE_SYSTEM = 3;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserSkillMapper userSkillMapper;

    @Override
    public IPage<SystemSkillVO> listEnabled(String keyword, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        LambdaQueryWrapper<UserSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkill::getSourceType, SOURCE_TYPE_SYSTEM)
                .eq(UserSkill::getEnableStatus, 1)
                .eq(UserSkill::getIsDeleted, 0)
                .orderByAsc(UserSkill::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(UserSkill::getSkillName, keyword.trim());
        }
        Page<UserSkill> rowPage = new Page<>(safePage, safeSize);
        IPage<UserSkill> result = userSkillMapper.selectPage(rowPage, wrapper);
        List<SystemSkillVO> records = result.getRecords().stream()
                .map(this::toVo)
                .collect(Collectors.toList());
        Page<SystemSkillVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    private SystemSkillVO toVo(UserSkill skill) {
        SystemSkillVO vo = new SystemSkillVO();
        vo.setBizNo(skill.getBizNo());
        vo.setName(skill.getSkillName());
        vo.setDescription(skill.getDescription());
        vo.setPromptSummary(skill.getPromptSummary());
        vo.setPrompt(skill.getPrompt());
        vo.setScope(skill.getScope());
        return vo;
    }
}