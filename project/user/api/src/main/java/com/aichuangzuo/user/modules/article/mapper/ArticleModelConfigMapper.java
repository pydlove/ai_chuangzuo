package com.aichuangzuo.user.modules.article.mapper;

import com.aichuangzuo.user.modules.article.dto.ActiveModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户端只读 a_model_config（管理端表）：AI 标题优化取 1 条 active 模型配置。
 * 与 generation 模块的 GenerationActiveModelConfigMapper 同一模式，不引入管理端实体。
 */
@Mapper
public interface ArticleModelConfigMapper {

    @Select("SELECT id, provider_type AS providerType, model_code AS modelCode, "
            + "base_url AS baseUrl, api_key_encrypted AS apiKeyEncrypted "
            + "FROM a_model_config WHERE is_active = 1 AND is_deleted = 0 ORDER BY id ASC LIMIT 1")
    ActiveModelConfig selectActive();
}
