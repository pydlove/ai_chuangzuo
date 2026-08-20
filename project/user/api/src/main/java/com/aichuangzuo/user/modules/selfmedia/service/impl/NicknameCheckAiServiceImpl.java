package com.aichuangzuo.user.modules.selfmedia.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.selfmedia.enums.SelfMediaPlanErrorCode;
import com.aichuangzuo.user.modules.selfmedia.service.NicknameCheckAiService;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanAiService;
import com.aichuangzuo.user.modules.selfmedia.vo.NicknameCheckVO;
import com.aichuangzuo.user.modules.selfmedia.vo.NicknameRecommendVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NicknameCheckAiServiceImpl implements NicknameCheckAiService {

    private static final String CHECK_PROMPT_CODE = "platform_account_check_v1";
    private static final String RECOMMEND_PROMPT_CODE = "platform_account_recommend_v1";

    private final SelfMediaPlanAiService selfMediaPlanAiService;

    @Override
    public NicknameCheckVO checkNickname(String platform, String positioning, String nickname) {
        if (StringUtils.isAnyBlank(platform, positioning, nickname)) {
            throw new BusinessException(SelfMediaPlanErrorCode.NICKNAME_CHECK_AI_FAILED);
        }
        Map<String, Object> variables = Map.of(
                "platform", platform.trim(),
                "positioning", positioning.trim(),
                "nickname", nickname.trim()
        );
        JsonNode result = selfMediaPlanAiService.callPrompt(CHECK_PROMPT_CODE, variables);
        return parseCheckResult(result);
    }

    @Override
    public NicknameRecommendVO recommendNickname(String platform, String positioning) {
        if (StringUtils.isAnyBlank(platform, positioning)) {
            throw new BusinessException(SelfMediaPlanErrorCode.NICKNAME_CHECK_AI_FAILED);
        }
        Map<String, Object> variables = Map.of(
                "platform", platform.trim(),
                "positioning", positioning.trim()
        );
        JsonNode result = selfMediaPlanAiService.callPrompt(RECOMMEND_PROMPT_CODE, variables);
        return parseRecommendResult(result);
    }

    private NicknameCheckVO parseCheckResult(JsonNode result) {
        NicknameCheckVO vo = new NicknameCheckVO();
        if (result == null) {
            throw new BusinessException(SelfMediaPlanErrorCode.NICKNAME_CHECK_AI_FAILED);
        }
        JsonNode fitNode = result.path("fit");
        if (fitNode.isBoolean()) {
            vo.setFit(fitNode.asBoolean());
        } else {
            vo.setFit(false);
        }
        vo.setReason(textOrDefault(result.path("reason"), "暂未给出判定理由"));
        vo.setSuggestions(parseSuggestions(result.path("suggestions")));
        if (Boolean.FALSE.equals(vo.getFit()) && vo.getSuggestions().isEmpty()) {
            log.warn("昵称检测 AI 返回不契合但未给出建议, result={}", result);
            vo.setSuggestions(List.of(
                    platformNameOf(result) + "笔记",
                    result.path("nickname").asText("") + "的成长日记",
                    result.path("nickname").asText("") + "说"
            ));
        }
        return vo;
    }

    private NicknameRecommendVO parseRecommendResult(JsonNode result) {
        NicknameRecommendVO vo = new NicknameRecommendVO();
        if (result == null) {
            throw new BusinessException(SelfMediaPlanErrorCode.NICKNAME_CHECK_AI_FAILED);
        }
        vo.setNickname(textOrDefault(result.path("nickname"), "未生成昵称"));
        vo.setBio(textOrDefault(result.path("bio"), "暂未生成简介"));
        return vo;
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        return node.isMissingNode() || !node.isTextual() || StringUtils.isBlank(node.asText())
                ? defaultValue
                : node.asText().trim();
    }

    private List<String> parseSuggestions(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    String text = item.asText().trim();
                    if (StringUtils.isNotBlank(text)) {
                        list.add(text);
                    }
                }
            }
        }
        return list;
    }

    private String platformNameOf(JsonNode result) {
        JsonNode platformNode = result.path("platform");
        return platformNode.isTextual() ? platformNode.asText("").trim() : "";
    }
}

