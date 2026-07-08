package com.aichuangzuo.user.modules.style.service.impl;

import com.aichuangzuo.user.modules.style.entity.UserStyle;
import com.aichuangzuo.user.modules.style.mapper.UserStyleMapper;
import com.aichuangzuo.user.modules.style.service.SystemStyleService;
import com.aichuangzuo.user.modules.style.vo.SystemStyleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统预设风格服务实现。
 *
 * <p>只读视图：从 {@code u_user_style} 过滤 {@code source_type=3 AND enable_status=1 AND is_deleted=0}。
 * 不维护：增删改由管理端负责。
 */
@Service
@RequiredArgsConstructor
public class SystemStyleServiceImpl implements SystemStyleService {

    private static final int SOURCE_TYPE_SYSTEM = 3;

    private final UserStyleMapper userStyleMapper;

    @Override
    public List<SystemStyleVO> listEnabled(String keyword) {
        LambdaQueryWrapper<UserStyle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStyle::getSourceType, SOURCE_TYPE_SYSTEM)
                .eq(UserStyle::getEnableStatus, 1)
                .eq(UserStyle::getIsDeleted, 0)
                .orderByAsc(UserStyle::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(UserStyle::getStyleName, keyword.trim());
        }
        return userStyleMapper.selectList(wrapper).stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    private SystemStyleVO toVo(UserStyle style) {
        SystemStyleVO vo = new SystemStyleVO();
        vo.setBizNo(style.getBizNo());
        vo.setName(style.getStyleName());
        vo.setDescription(style.getDescription());
        vo.setPromptSummary(style.getPromptSummary());
        vo.setPrompt(style.getPrompt());
        vo.setScope(style.getScope());
        return vo;
    }
}