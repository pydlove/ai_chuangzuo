package com.aichuangzuo.user.modules.selfmedia.vo;

import com.aichuangzuo.user.modules.selfmedia.dto.QuestionAnswerDTO;
import lombok.Data;

import java.util.List;

@Data
public class SelfMediaPlanVO {
    private String platformKey;
    private String platformName;
    private String nicheKey;
    private String nicheName;
    private String personaKey;
    private String personaName;
    private List<PillarVO> pillars;
    private List<QuestionAnswerDTO> answers;
}
