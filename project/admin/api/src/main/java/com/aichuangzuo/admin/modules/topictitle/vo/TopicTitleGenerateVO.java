package com.aichuangzuo.admin.modules.topictitle.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * AI 生成标题结果：实际入库条数（无效条目被剔除后可能少于请求数量）。
 */
@Data
@AllArgsConstructor
public class TopicTitleGenerateVO {

    private int generated;
}
