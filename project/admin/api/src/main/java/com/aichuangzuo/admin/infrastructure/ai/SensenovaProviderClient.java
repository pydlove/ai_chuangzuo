package com.aichuangzuo.admin.infrastructure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 商汤 SenseNova 客户端：OpenAI 兼容协议，版本路径前缀 /v1
 * （如 https://token.sensenova.cn/v1/chat/completions）。
 *
 * <p>测试连接 / 拉取模型列表 / 问答测试全部复用 {@link KimiProviderClient} 的实现，
 * 与 Kimi 的差异仅在于语义归属，便于后续按厂商定制（如模型列表字段差异）。
 */
@Slf4j
@Component
public class SensenovaProviderClient extends KimiProviderClient {

    @Override
    protected String apiPrefix() {
        return "/v1";
    }
}
