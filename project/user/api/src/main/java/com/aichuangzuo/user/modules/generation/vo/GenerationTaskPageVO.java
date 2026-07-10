package com.aichuangzuo.user.modules.generation.vo;

import lombok.Data;

import java.util.List;

/** 列表分页响应。 */
@Data
public class GenerationTaskPageVO {
    private List<GenerationTaskVO> list;
    private long total;
    private long page;
    private long pageSize;
}
