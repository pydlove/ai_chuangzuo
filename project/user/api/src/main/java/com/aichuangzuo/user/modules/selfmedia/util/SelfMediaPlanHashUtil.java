package com.aichuangzuo.user.modules.selfmedia.util;

import com.aichuangzuo.user.modules.selfmedia.dto.QuestionAnswerDTO;
import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlan;
import com.aichuangzuo.user.modules.selfmedia.vo.PillarVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自媒体运营方案内容哈希工具。
 *
 * <p>用于发布建议缓存：只要影响发布建议 AI  prompt 的运营方案字段不变，
 * 计算出的哈希就不变，从而可以直接命中缓存，无需重复调用 AI。
 */
@Slf4j
public final class SelfMediaPlanHashUtil {

    private SelfMediaPlanHashUtil() {
        // utility class
    }

    /**
     * 基于运营方案完整内容计算 SHA-256 哈希。
     *
     * <p>包含：平台、赛道、人设、内容支柱、问卷答案。字段顺序和内部数组顺序固定，
     * 保证内容相同则哈希相同。
     */
    public static String computePlanContentHash(ObjectMapper objectMapper, SelfMediaPlan plan) {
        try {
            Map<String, Object> canonical = new LinkedHashMap<>();
            canonical.put("platformKey", StringUtils.defaultString(plan.getPlatformKey()));
            canonical.put("platformName", StringUtils.defaultString(plan.getPlatformName()));
            canonical.put("nicheKey", StringUtils.defaultString(plan.getNicheKey()));
            canonical.put("nicheName", StringUtils.defaultString(plan.getNicheName()));
            canonical.put("personaKey", StringUtils.defaultString(plan.getPersonaKey()));
            canonical.put("personaName", StringUtils.defaultString(plan.getPersonaName()));
            canonical.put("pillars", canonicalPillars(objectMapper, plan.getContentPillarsJson()));
            canonical.put("answers", canonicalAnswers(objectMapper, plan.getAnswersJson()));

            String json = objectMapper.writeValueAsString(canonical);
            return sha256(json);
        } catch (Exception e) {
            log.warn("[运营方案] 内容哈希计算失败，使用降级哈希 userId={}", plan.getUserId(), e);
            return fallbackHash(plan);
        }
    }

    private static List<Map<String, Object>> canonicalPillars(ObjectMapper objectMapper, String json) {
        List<PillarVO> pillars = parseJson(objectMapper, json, new TypeReference<>() {});
        if (pillars == null) {
            return new ArrayList<>();
        }
        return pillars.stream()
                .filter(p -> StringUtils.isNotBlank(p.getName()))
                .sorted(Comparator.comparing(PillarVO::getName)
                        .thenComparing(p -> p.getPercent() == null ? 0 : p.getPercent()))
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", p.getName().trim());
                    m.put("percent", p.getPercent() == null ? 0 : p.getPercent());
                    return m;
                })
                .toList();
    }

    private static List<Map<String, String>> canonicalAnswers(ObjectMapper objectMapper, String json) {
        List<QuestionAnswerDTO> answers = parseJson(objectMapper, json, new TypeReference<>() {});
        if (answers == null) {
            return new ArrayList<>();
        }
        return answers.stream()
                .filter(a -> StringUtils.isNotBlank(a.getQuestionKey()))
                .sorted(Comparator.comparing(QuestionAnswerDTO::getQuestionKey))
                .map(a -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("questionKey", a.getQuestionKey().trim());
                    m.put("answer", StringUtils.defaultString(a.getAnswer()).trim());
                    return m;
                })
                .toList();
    }

    private static <T> T parseJson(ObjectMapper objectMapper, String json, TypeReference<T> typeRef) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("[运营方案] JSON 解析失败，跳过该字段 hash 计算 json={}", json);
            return null;
        }
    }

    private static String fallbackHash(SelfMediaPlan plan) {
        String raw = StringUtils.defaultString(plan.getPlatformKey()) + "|"
                + StringUtils.defaultString(plan.getPlatformName()) + "|"
                + StringUtils.defaultString(plan.getNicheKey()) + "|"
                + StringUtils.defaultString(plan.getNicheName()) + "|"
                + StringUtils.defaultString(plan.getPersonaKey()) + "|"
                + StringUtils.defaultString(plan.getPersonaName()) + "|"
                + StringUtils.defaultString(plan.getContentPillarsJson()) + "|"
                + StringUtils.defaultString(plan.getAnswersJson());
        return sha256(raw);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
