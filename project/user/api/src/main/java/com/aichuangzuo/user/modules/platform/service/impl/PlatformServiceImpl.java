package com.aichuangzuo.user.modules.platform.service.impl;

import com.aichuangzuo.shared.entity.Platform;
import com.aichuangzuo.user.modules.platform.mapper.PlatformMapper;
import com.aichuangzuo.user.modules.platform.service.PlatformService;
import com.aichuangzuo.user.modules.platform.vo.PlatformVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 用户端自媒体平台配置查询服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformServiceImpl implements PlatformService {

    private final PlatformMapper platformMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<PlatformVO> listEnabled() {
        List<Platform> list = platformMapper.selectList(
                new LambdaQueryWrapper<Platform>()
                        .eq(Platform::getStatus, 1)
                        .orderByAsc(Platform::getSortOrder));
        return list.stream().map(this::toVO).toList();
    }

    private PlatformVO toVO(Platform p) {
        PlatformVO vo = new PlatformVO();
        vo.setId(p.getId());
        vo.setPlatformKey(p.getPlatformKey());
        vo.setPlatformName(p.getPlatformName());
        vo.setDescription(p.getDescription());
        vo.setRecommendWords(p.getRecommendWords());
        vo.setTrait(p.getTrait());
        vo.setIsDefault(Integer.valueOf(1).equals(p.getIsDefault()));
        vo.setIconUrl(p.getIconUrl());
        vo.setWordCountPresets(Collections.emptyList());
        vo.setTagline(p.getTagline());
        vo.setContentForm(parseJsonList(p.getContentFormJson()));
        vo.setMonetization(parseJsonList(p.getMonetizationJson()));
        vo.setThreshold(p.getThreshold());
        vo.setBestFor(p.getBestFor());
        vo.setReason(p.getReason());
        vo.setMonetizationEase(p.getMonetizationEase());
        vo.setTimeToIncome(p.getTimeToIncome());
        vo.setIncomeRange(p.getIncomeRange());
        vo.setDifficulty(p.getDifficulty());
        return vo;
    }

    @SneakyThrows
    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    }
}
