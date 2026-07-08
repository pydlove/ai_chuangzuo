package com.aichuangzuo.user.modules.style.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.style.dto.request.CreateStyleRequest;
import com.aichuangzuo.user.modules.style.dto.request.UpdateStyleRequest;
import com.aichuangzuo.user.modules.style.entity.UserStyle;
import com.aichuangzuo.user.modules.style.enums.StyleErrorCode;
import com.aichuangzuo.user.modules.style.mapper.UserStyleMapper;
import com.aichuangzuo.user.modules.style.service.UserStyleService;
import com.aichuangzuo.user.modules.style.vo.UserStyleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
public class UserStyleServiceImpl implements UserStyleService {

    private static final int MAX_SCOPE_TAGS = 3;
    private static final int MAX_SCOPE_TAG_LENGTH = 8;
    private static final int SOURCE_TYPE_CUSTOM = 1;

    private final UserStyleMapper userStyleMapper;

    @Override
    public List<UserStyleVO> listMyStyles(Integer sourceType) {
        Long userId = SecurityUserContext.getCurrentUserId();
        LambdaQueryWrapper<UserStyle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStyle::getUserId, userId)
                .eq(UserStyle::getSourceType, sourceType == null ? SOURCE_TYPE_CUSTOM : sourceType)
                .orderByDesc(UserStyle::getUpdatedAt);
        return userStyleMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public UserStyleVO createStyle(CreateStyleRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        String styleName = request.getStyleName().trim();
        String prompt = request.getPrompt().trim();
        String scope = normalizeScope(request.getScope());

        validateScope(scope);
        ensureNameNotExists(userId, styleName, null);

        UserStyle style = new UserStyle();
        style.setBizNo(generateBizNo());
        style.setUserId(userId);
        style.setStyleName(styleName);
        style.setPrompt(prompt);
        style.setScope(scope);
        style.setSourceType(SOURCE_TYPE_CUSTOM);
        style.setAuditStatus(0);
        style.setUseCount(0);

        userStyleMapper.insert(style);
        log.info("创建风格成功 userId={}, bizNo={}, styleName={}", userId, style.getBizNo(), styleName);
        return toVO(style);
    }

    @Override
    public UserStyleVO updateStyle(String bizNo, UpdateStyleRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        UserStyle style = getOwnedStyle(bizNo, userId);

        String styleName = request.getStyleName().trim();
        String prompt = request.getPrompt().trim();
        String scope = normalizeScope(request.getScope());

        validateScope(scope);
        ensureNameNotExists(userId, styleName, style.getId());

        style.setStyleName(styleName);
        style.setPrompt(prompt);
        style.setScope(scope);

        userStyleMapper.updateById(style);
        log.info("更新风格成功 userId={}, bizNo={}, styleName={}", userId, bizNo, styleName);
        return toVO(style);
    }

    @Override
    public void deleteStyle(String bizNo) {
        Long userId = SecurityUserContext.getCurrentUserId();
        UserStyle style = getOwnedStyle(bizNo, userId);
        userStyleMapper.deleteById(style.getId());
        log.info("删除风格成功 userId={}, bizNo={}", userId, bizNo);
    }

    /**
     * 按业务编号获取当前用户的风格；不存在或无权限时抛异常。
     */
    private UserStyle getOwnedStyle(String bizNo, Long userId) {
        LambdaQueryWrapper<UserStyle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStyle::getBizNo, bizNo)
                .eq(UserStyle::getUserId, userId);
        UserStyle style = userStyleMapper.selectOne(wrapper);
        if (style == null) {
            throw new BusinessException(StyleErrorCode.STYLE_NOT_FOUND);
        }
        return style;
    }

    /**
     * 校验同一用户下风格名是否重复。
     *
     * @param excludeId 排除的风格主键（更新时使用）
     */
    private void ensureNameNotExists(Long userId, String styleName, Long excludeId) {
        LambdaQueryWrapper<UserStyle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStyle::getUserId, userId)
                .eq(UserStyle::getStyleName, styleName);
        if (excludeId != null) {
            wrapper.ne(UserStyle::getId, excludeId);
        }
        Long count = userStyleMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(StyleErrorCode.STYLE_NAME_EXISTS);
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
            throw new BusinessException(StyleErrorCode.STYLE_SCOPE_TOO_LONG);
        }
        for (String tag : tags) {
            if (tag.length() > MAX_SCOPE_TAG_LENGTH) {
                throw new BusinessException(StyleErrorCode.STYLE_SCOPE_TOO_LONG);
            }
        }
    }

    private String generateBizNo() {
        return "S" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private UserStyleVO toVO(UserStyle style) {
        if (style == null) {
            return null;
        }
        UserStyleVO vo = new UserStyleVO();
        vo.setBizNo(style.getBizNo());
        vo.setStyleName(style.getStyleName());
        vo.setPrompt(style.getPrompt());
        vo.setDescription(style.getDescription());
        vo.setPromptSummary(style.getPromptSummary());
        vo.setScope(style.getScope());
        vo.setEnableStatus(style.getEnableStatus());
        vo.setSourceType(style.getSourceType());
        vo.setUseCount(style.getUseCount());
        vo.setCreatedAt(style.getCreatedAt());
        vo.setUpdatedAt(style.getUpdatedAt());
        vo.setAuditStatus(style.getAuditStatus());
        vo.setRejectReason(style.getRejectReason());
        return vo;
    }
}
