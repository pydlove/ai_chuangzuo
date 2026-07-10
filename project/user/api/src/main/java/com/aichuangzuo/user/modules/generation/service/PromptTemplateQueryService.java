package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.shared.creative.CreativeTemplateConstants;
import com.aichuangzuo.shared.creative.TemplateStatus;
import com.aichuangzuo.shared.entity.PromptTemplate;
import com.aichuangzuo.shared.enums.error.UserGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.generation.mapper.UserPromptTemplateMapper;
import com.aichuangzuo.user.modules.generation.vo.PromptTemplatePublicVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * User 端-创作模板查询服务（只读）。
 *
 * <p>仅返回 status=PUBLISHED 的模板，prompt 全文不暴露给前端。
 *
 * <p>设计文档：§5.15.4
 */
@Service
@RequiredArgsConstructor
public class PromptTemplateQueryService {

    private final UserPromptTemplateMapper mapper;

    public List<PromptTemplatePublicVO> listPublished() {
        return mapper.selectPublished().stream()
                .map(this::toVo)
                .toList();
    }

    public PromptTemplatePublicVO detail(Long id) {
        PromptTemplate t = mapper.selectById(id);
        if (t == null
                || t.getTemplateStatus() == null
                || t.getTemplateStatus() != TemplateStatus.PUBLISHED.code) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_TEMPLATE_DISABLED);
        }
        return toVo(t);
    }

    private PromptTemplatePublicVO toVo(PromptTemplate t) {
        PromptTemplatePublicVO vo = new PromptTemplatePublicVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setRemark(t.getRemark());
        vo.setLatestPublishedVersion(t.getLatestPublishedVersion());
        vo.setIsBuiltin(t.getId() != null && t.getId() == CreativeTemplateConstants.DEFAULT_TEMPLATE_ID);
        return vo;
    }
}