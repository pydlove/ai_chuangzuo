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
 * 模拟运营-机器人资料生成器：用 LLM 生成昵称与签名，文本做清洗后返回。
 *
 * <p>模型选择沿用 model_config：取 is_active=1 且优先级最高的一条。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationProfileGenerator {

    private final GenerationAiService generationAiService;
    private final ModelConfigMapper modelConfigMapper;

    /** 随机风格标签：让不同机器人的昵称风格分散，避免扎堆"晚风/拾光"类近似词。 */
    private static final String[] STYLE_HINTS = {
            "像一个分享日常美食的博主", "像一个科技数码爱好者", "像一个到处旅行的背包客",
            "像一个记录职场成长的打工人", "像一个爱读书的文化人", "像一个带娃的日常博主",
            "像一个健身运动爱好者", "像一个爱讲冷笑话的段子手", "像一个街头摄影爱好者",
            "像一个音乐发烧友", "像一个手作DIY达人", "像一个炒股理财老韭菜",
            "像一个养猫养狗铲屎官", "像一个爱种花的园艺爱好者", "像一个追剧的影视解说",
            "像一个穿搭博主", "像一个钓鱼佬", "像一个咖啡爱好者",
            "像一个历史人文爱好者", "像一个英语学习打卡者"
    };

    /**
     * 生成昵称；excludeNames 为已用过的昵称（禁止重复，也避免与其用字过于相近）。
     */
    public String generateNickname(java.util.List<String> excludeNames) {
        String style = STYLE_HINTS[java.util.concurrent.ThreadLocalRandom.current().nextInt(STYLE_HINTS.length)];
        String avoid = "";
        if (excludeNames != null && !excludeNames.isEmpty()) {
            int end = Math.min(excludeNames.size(), 30);
            avoid = "以下昵称已被使用，绝对禁止使用其中的任何字词组合，也禁止起与它们风格雷同的名字："
                    + String.join("、", excludeNames.subList(0, end)) + "。";
        }
        return call(
                "你是网名生成器。只输出昵称本身，不要引号、不要解释、不要标点。",
                "生成1个中文网名，2-6个字，" + style + "，有网感但不低俗。" + avoid + "直接输出昵称。");
    }

    public String generateBio(String nickname) {
        return call(
                "你是个人简介生成器。只输出简介本身，不要引号、不要解释。",
                "博主昵称「" + nickname + "」。为其写一句中文个人简介，20-40字，"
                        + "像一个真实博主会写在个人主页的签名，有温度、有吸引力，可以适度用 emoji 或斜杠，"
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
