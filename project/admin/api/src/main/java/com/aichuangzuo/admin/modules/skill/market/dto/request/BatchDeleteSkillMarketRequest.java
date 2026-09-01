package com.aichuangzuo.admin.modules.skill.market.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量删除提示词市场条目请求体。
 */
@Data
public class BatchDeleteSkillMarketRequest {

    @NotEmpty(message = "请选择要删除的条目")
    @Size(max = 100, message = "一次最多删除 100 条")
    private List<String> bizNos;
}
