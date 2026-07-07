package com.aichuangzuo.user.modules.leaderboard.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收入申报提交请求。
 */
@Data
public class IncomeSubmissionUploadRequest {

    /** 申报月份，格式 YYYY-MM。 */
    private String periodMonth;

    /** 申报金额（元）。 */
    private BigDecimal amount;

    /** 自媒体平台：wechat / xiaohongshu / douyin / other。 */
    private String platform;

    /** 截图相对路径列表，由上传接口返回。 */
    private List<String> screenshotPaths;
}
