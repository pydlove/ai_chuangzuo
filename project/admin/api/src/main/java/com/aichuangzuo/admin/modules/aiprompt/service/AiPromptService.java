package com.aichuangzuo.admin.modules.aiprompt.service;

import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptCreateRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptQueryRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptUpdateRequest;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptDetailVO;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptVO;

import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptTestVO;

import java.util.List;
import java.util.Map;

public interface AiPromptService {

    PageResult list(AiPromptQueryRequest request);
    List<String> listCategories();
    AiPromptDetailVO get(Long id);
    Long create(AiPromptCreateRequest request);
    void update(Long id, AiPromptUpdateRequest request);
    void enable(Long id);
    void disable(Long id);
    AiPromptTestVO test(Long id, Map<String, Object> variables);

    record PageResult(List<AiPromptVO> list, long total, long page, long pageSize) {}
}
