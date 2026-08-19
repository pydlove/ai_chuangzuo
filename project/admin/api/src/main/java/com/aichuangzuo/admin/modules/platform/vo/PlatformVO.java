package com.aichuangzuo.admin.modules.platform.vo;

import com.aichuangzuo.shared.entity.Platform;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 自媒体平台配置 VO（管理端）。
 */
@Data
@Slf4j
public class PlatformVO {

    private Long id;
    private String platformKey;
    private String platformName;
    private String description;
    private Integer recommendWords;
    private String trait;
    private Integer sortOrder;
    private Integer status;
    private Integer isDefault;
    private String iconUrl;
    private List<PlatformWordCountPresetVO> wordCountPresets;

    private String tagline;
    private List<String> contentForm;
    private List<String> monetization;
    private String threshold;
    private String bestFor;
    private String reason;
    private String monetizationEase;
    private String timeToIncome;
    private String incomeRange;
    private String difficulty;

    public static PlatformVO from(Platform platform, ObjectMapper objectMapper) {
        PlatformVO vo = new PlatformVO();
        vo.setId(platform.getId());
        vo.setPlatformKey(platform.getPlatformKey());
        vo.setPlatformName(platform.getPlatformName());
        vo.setDescription(platform.getDescription());
        vo.setRecommendWords(platform.getRecommendWords());
        vo.setTrait(platform.getTrait());
        vo.setSortOrder(platform.getSortOrder());
        vo.setStatus(platform.getStatus());
        vo.setIsDefault(platform.getIsDefault());
        vo.setIconUrl(platform.getIconUrl());
        if (platform.getWordCountPresetsJson() != null) {
            try {
                vo.setWordCountPresets(objectMapper.readValue(platform.getWordCountPresetsJson(),
                        new TypeReference<>() {}));
            } catch (Exception e) {
                log.warn("解析平台字数配置失败 platformKey={}", platform.getPlatformKey(), e);
                vo.setWordCountPresets(Collections.emptyList());
            }
        } else {
            vo.setWordCountPresets(Collections.emptyList());
        }
        vo.setTagline(platform.getTagline());
        vo.setContentForm(parseJsonList(platform.getContentFormJson(), objectMapper,
                platform.getPlatformKey(), "内容形式"));
        vo.setMonetization(parseJsonList(platform.getMonetizationJson(), objectMapper,
                platform.getPlatformKey(), "主要收益"));
        vo.setThreshold(platform.getThreshold());
        vo.setBestFor(platform.getBestFor());
        vo.setReason(platform.getReason());
        vo.setMonetizationEase(platform.getMonetizationEase());
        vo.setTimeToIncome(platform.getTimeToIncome());
        vo.setIncomeRange(platform.getIncomeRange());
        vo.setDifficulty(platform.getDifficulty());
        return vo;
    }

    private static List<String> parseJsonList(String json, ObjectMapper objectMapper,
                                                String platformKey, String fieldName) {
        if (json == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析平台{}失败 platformKey={}", fieldName, platformKey, e);
            return Collections.emptyList();
        }
    }
}
