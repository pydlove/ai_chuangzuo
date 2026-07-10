package com.aichuangzuo.user.modules.generation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户端只读 a_model_config（管理端表）：提交任务时挑 1 条 active 模型配置。
 * 不引入完整实体，只用 SQL 拿 id。
 */
@Mapper
public interface GenerationActiveModelConfigMapper {

    @Select("SELECT id FROM a_model_config WHERE is_active = 1 AND is_deleted = 0 ORDER BY id ASC LIMIT 1")
    Long selectActiveId();
}
