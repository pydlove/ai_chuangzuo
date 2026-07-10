package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;

import java.util.List;

@Data
public class GenerationTaskAdminPageVO {
    private List<GenerationTaskAdminVO> list;
    private long total;
    private long page;
    private long pageSize;
}
