package com.aichuangzuo.admin.modules.commission.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class CommissionTaskExcelRowData {

    @ExcelProperty("序号")
    private String seqNo;

    @ExcelProperty("任务标题（必填）")
    private String title;

    @ExcelProperty("需求描述（必填）")
    private String description;

    @ExcelProperty("最小字数（必填）")
    private String minWordCount;

    @ExcelProperty("最大字数（必填）")
    private String maxWordCount;

    @ExcelProperty("风格提示（选填）")
    private String skillHint;

    @ExcelProperty("每篇奖励/创作币（必填）")
    private String rewardCoin;

    @ExcelProperty("需采纳数量/篇（必填）")
    private String neededCount;

    @ExcelProperty("投递截止时间（必填）")
    private String deadlineAt;

    @ExcelProperty("评选截止时间（必填）")
    private String selectionDeadlineAt;
}
