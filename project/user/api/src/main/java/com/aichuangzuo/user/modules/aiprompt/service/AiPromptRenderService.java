package com.aichuangzuo.user.modules.aiprompt.service;

import com.aichuangzuo.shared.vo.AiPromptRendered;

import java.util.Map;

public interface AiPromptRenderService {

    AiPromptRendered render(String promptCode, Map<String, Object> variables);
}
