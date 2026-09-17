package com.aichuangzuo.admin.modules.simulation.service;

import com.aichuangzuo.admin.modules.generation.service.AiCallResult;
import com.aichuangzuo.admin.modules.generation.service.GenerationAiService;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 模拟运营-机器人签名生成器：昵称取自昵称库，签名仍由 LLM 按昵称生成，文本清洗后返回。
 *
 * <p>模型选择沿用 model_config：取 is_active=1 且优先级最高的一条。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationProfileGenerator {

    private final GenerationAiService generationAiService;
    private final ModelConfigMapper modelConfigMapper;

    public String generateBio(String nickname) {
        return call(
                "你是个人简介生成器。只输出简介本身，不要引号、不要解释。",
                "用户昵称「" + nickname + "」。为其写一句中文个人简介，20-40字，"
                        + "像一个真实用户会写在个人主页的签名，有温度、有生活气息，可以适度用 emoji 或斜杠，"
                        + "但不要使用竖线等列表式分隔符。直接输出简介。");
    }

    private String call(String system, String user) {
        ModelConfig cfg = modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getIsActive, 1)
                .orderByDesc(ModelConfig::getPriority)
                .orderByAsc(ModelConfig::getId)
                .last("LIMIT 1"));
        if (cfg == null) {
            throw new BusinessException(500, "模拟运营无可用模型配置");
        }
        AiCallResult result = generationAiService.call(cfg.getId(), system, user, null, false);
        String content = result.getContent();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(500, "模拟运营模型返回内容为空");
        }
        return clean(content.trim());
    }

    /** 去掉模型可能带出的引号、首尾空白与换行。 */
    private static String clean(String s) {
        String t = s;
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("“") && t.endsWith("”"))) {
            t = t.substring(1, t.length() - 1);
        }
        t = t.replace("\n", " ").replace("\r", " ").trim();
        return t;
    }
}
