package com.aichuangzuo.user.modules.selfmedia.dto.request;

import com.aichuangzuo.user.modules.selfmedia.dto.QuestionAnswerDTO;
import lombok.Data;

import java.util.List;

@Data
public class RecommendPersonasRequest {
    private String platformKey;
    private String nicheKey;
    private List<QuestionAnswerDTO> answers;
}
