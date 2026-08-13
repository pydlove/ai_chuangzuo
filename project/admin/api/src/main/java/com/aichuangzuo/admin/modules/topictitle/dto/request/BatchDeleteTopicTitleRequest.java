package com.aichuangzuo.admin.modules.topictitle.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除标题请求。
 */
@Data
public class BatchDeleteTopicTitleRequest {

    /** 待删除标题 ID 列表；会逐条加载实体后逻辑删除，保留 updated_by 原值。 */
    @NotEmpty(message = "请至少选择一条标题")
    private List<Long> ids;
}
