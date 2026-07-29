package com.aichuangzuo.user.modules.skill.vo;

import lombok.Data;

/**
 * 风格分析结果。
 */
@Data
public class SkillAnalyzeVO {

    /** 原文代表性片段 1（≤120字；模型未逐字命中原文时降级为首段截取）。 */
    private String excerpt1;

    /** 原文代表性片段 2（≤80字；模型未逐字命中原文时降级为最长句截取）。 */
    private String excerpt2;

    /** 四段式 skill 提示词（≤1200 字，可直接入库 u_user_skill.prompt）。 */
    private String prompt;
}
