package com.aichuangzuo.user.modules.selfmedia.dto.request;

import com.aichuangzuo.user.modules.selfmedia.dto.QuestionAnswerDTO;
import lombok.Data;

import java.util.List;

@Data
public class RecommendNichesRequest {
    private String platformKey;
    private List<QuestionAnswerDTO> answers;
}
