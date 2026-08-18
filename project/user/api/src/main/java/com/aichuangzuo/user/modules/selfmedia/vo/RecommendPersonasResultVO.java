package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

import java.util.List;

@Data
public class RecommendPersonasResultVO {
    private List<PersonaOptionVO> personas;
    private List<PillarVO> defaultPillars;
}
