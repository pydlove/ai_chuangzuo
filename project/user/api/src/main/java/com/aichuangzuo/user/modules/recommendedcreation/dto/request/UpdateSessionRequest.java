package com.aichuangzuo.user.modules.recommendedcreation.dto.request;

import com.aichuangzuo.user.modules.recommendedcreation.vo.AngleOptionVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateSessionRequest {

    @NotNull(message = "当前步骤不能为空")
    @Min(1)
    @Max(5)
    private Integer currentStep;

    @Min(100)
    @Max(3000)
    private Integer wordCount;

    @Size(max = 4000, message = "提示词长度不能超过 4000 字符")
    private String prompt;

    @Size(max = 64)
    private String template;

    private List<AngleOptionVO> selectedAngles;
}
