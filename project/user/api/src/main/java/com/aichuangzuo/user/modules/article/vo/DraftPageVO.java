package com.aichuangzuo.user.modules.article.vo;

import lombok.Data;

import java.util.List;

/**
 * 草稿分页 VO。
 */
@Data
public class DraftPageVO {

    private List<DraftVO> list;

    private long total;

    private long page;

    private long pageSize;
}