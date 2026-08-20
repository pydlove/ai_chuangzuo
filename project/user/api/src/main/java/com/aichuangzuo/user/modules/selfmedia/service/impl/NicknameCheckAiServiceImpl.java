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
            String nickname = result.path("nickname").asText("");
            vo.setSuggestions(List.of(
                    suggestionOf(platformNameOf(result) + "笔记", ""),
                    suggestionOf(nickname + "的成长日记", ""),
                    suggestionOf(nickname + "说", "")
            ));
        }
        return vo;
    }

    private NicknameRecommendVO parseRecommendResult(JsonNode result) {
        NicknameRecommendVO vo = new NicknameRecommendVO();
        if (result == null) {
            throw new BusinessException(SelfMediaPlanErrorCode.NICKNAME_CHECK_AI_FAILED);
        }
        List<NicknameRecommendVO.Option> options = new ArrayList<>();
        JsonNode optionsNode = result.path("options");
        if (optionsNode.isArray()) {
            for (JsonNode item : optionsNode) {
                NicknameRecommendVO.Option option = new NicknameRecommendVO.Option();
                option.setNickname(textOrDefault(item.path("nickname"), "未生成昵称"));
                option.setBio(textOrDefault(item.path("bio"), "暂未生成简介"));
                options.add(option);
            }
        }
        if (options.isEmpty()) {
            NicknameRecommendVO.Option option = new NicknameRecommendVO.Option();
            option.setNickname(textOrDefault(result.path("nickname"), "未生成昵称"));
            option.setBio(textOrDefault(result.path("bio"), "暂未生成简介"));
            options.add(option);
        }
        vo.setOptions(options);
        return vo;
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        return node.isMissingNode() || !node.isTextual() || StringUtils.isBlank(node.asText())
                ? defaultValue
                : node.asText().trim();
    }

    private List<NicknameCheckVO.Suggestion> parseSuggestions(JsonNode node) {
        List<NicknameCheckVO.Suggestion> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    String text = item.asText().trim();
                    if (StringUtils.isNotBlank(text)) {
                        list.add(suggestionOf(text, ""));
                    }
                } else if (item.isObject()) {
                    String nickname = textOrDefault(item.path("nickname"), "");
                    if (StringUtils.isNotBlank(nickname)) {
                        list.add(suggestionOf(nickname, textOrDefault(item.path("bio"), "")));
                    }
                }
            }
        }
        return list;
    }

    private NicknameCheckVO.Suggestion suggestionOf(String nickname, String bio) {
        NicknameCheckVO.Suggestion s = new NicknameCheckVO.Suggestion();
        s.setNickname(nickname);
        s.setBio(bio);
        return s;
    }

    private String platformNameOf(JsonNode result) {
        JsonNode platformNode = result.path("platform");
        return platformNode.isTextual() ? platformNode.asText("").trim() : "";
    }
}

