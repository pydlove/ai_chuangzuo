package com.aichuangzuo.admin.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 智谱 GLM 客户端：OpenAI 兼容协议，与 Kimi 唯一差异是版本路径前缀 /v4
 * （如 https://open.bigmodel.cn/api/paas/v4/chat/completions）。
 *
 * <p>测试连接 / 拉取模型列表 / 问答测试全部复用 {@link KimiProviderClient} 的实现。
 */
@Slf4j
@Component
public class GlmProviderClient extends KimiProviderClient {

    @Override
    protected String apiPrefix() {
        return "/v4";
    }

    /**
     * 只填域名（https://open.bigmodel.cn）时补全官方路径 /api/paas，
     * 与 {@link com.aichuangzuo.admin.modules.generation.service.GenerationAiService} 的生成调用保持一致。
     */
    @Override
    protected String trim(String baseUrl) {
        String base = super.trim(baseUrl);
        int schemeEnd = base.indexOf("://");
        if (schemeEnd < 0 || base.indexOf('/', schemeEnd + 3) < 0) {
            return base + "/api/paas";
        }
        return base;
    }
}
