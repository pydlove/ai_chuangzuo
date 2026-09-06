package com.aichuangzuo.user.modules.article.mapper;

import com.aichuangzuo.shared.ai.ActiveModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户端只读 a_model_config（管理端表）：AI 同步调用取全部启用配置，交给共享
 * {@link com.aichuangzuo.shared.ai.ModelConfigSelector} 按「供应商轮询 → key 轮询」选下一个。
 * 不引入管理端实体。
 */
@Mapper
public interface ArticleModelConfigMapper {

    @Select("SELECT id, provider_type AS providerType, model_code AS modelCode, "
            + "base_url AS baseUrl, api_key_encrypted AS apiKeyEncrypted "
            + "FROM a_model_config WHERE is_active = 1 AND is_deleted = 0 ORDER BY priority ASC, id ASC")
    List<ActiveModelConfig> selectActiveList();
}
