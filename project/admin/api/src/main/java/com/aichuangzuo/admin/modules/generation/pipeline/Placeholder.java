package com.aichuangzuo.admin.modules.generation.pipeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阶段 prompt 中可用的占位符（前端用来渲染 chip + 后端渲染时替换）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Placeholder {

    /** 占位符 key（不含 {{ }}）。 */
    private String name;

    /** 简短说明。 */
    private String desc;
}
