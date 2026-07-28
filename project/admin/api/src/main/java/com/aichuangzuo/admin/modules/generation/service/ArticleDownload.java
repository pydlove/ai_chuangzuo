package com.aichuangzuo.admin.modules.generation.service;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 已完成任务的最终文章下载内容。
 */
@Data
@AllArgsConstructor
public class ArticleDownload {

    private String filename;
    private byte[] content;
}
