package com.aichuangzuo.shared.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 质量检测报告：流水线最后阶段让大模型对文章进行质量评估，结果随文章落库并展示给用户。
 *
 * <p>三项占比之和应为 100：aiRate + suspiciousRate + humanRate = 100。
 */
@Getter
@Setter
public class AiDetectReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 综合质量分数（0-100）。 */
    private Integer score;

    /** 质量等级：低 / 中 / 高 / 极高。 */
    private String qualityLevel;

    /** AI 特征占比（0-100），旧版兼容字段。 */
    private Integer aiRate = 0;

    /** 疑似 AI 占比（0-100），旧版兼容字段。 */
    private Integer suspiciousRate = 0;

    /** 人工特征占比（0-100），旧版兼容字段。 */
    private Integer humanRate = 0;

    /** 总体一句话评价。 */
    private String summary;

    /** 改进建议（3-5 条）。 */
    private List<String> suggestions = new ArrayList<>();

    /** 段落级评分（可选）。 */
    private List<ParagraphScore> paragraphScores = new ArrayList<>();

    @Getter
    @Setter
    public static class ParagraphScore implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer paragraphIndex;

        private Integer aiRate;

        private String reason;
    }
}
