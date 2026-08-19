package com.aichuangzuo.user.modules.selfmedia.dto.request;

import com.aichuangzuo.user.modules.selfmedia.dto.QuestionAnswerDTO;
import com.aichuangzuo.user.modules.selfmedia.vo.PillarVO;
import lombok.Data;

import java.util.List;

@Data
public class SavePlanRequest {
    private String platformKey;
    private String platformName;
    private String nicheKey;
    private String nicheName;
    private String personaKey;
    private String personaName;
    private List<PillarVO> pillars;
    private List<QuestionAnswerDTO> answers;
}
