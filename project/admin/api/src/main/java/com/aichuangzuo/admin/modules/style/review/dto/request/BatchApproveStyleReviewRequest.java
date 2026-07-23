package com.aichuangzuo.admin.modules.style.review.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量通过风格审核请求。
 */
@Data
public class BatchApproveStyleReviewRequest {

    /** 待通过的风格业务编号列表。 */
    @NotEmpty(message = "请选择要通过的风格")
    private List<String> bizNos;
}
