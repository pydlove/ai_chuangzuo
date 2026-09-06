package com.aichuangzuo.admin.modules.modelconfig.mapper;

import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.shared.ai.ActiveModelConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfig> {

    /**
     * 查询所有启用的配置，按优先级升序（数字越小越优先）。
     */
    @Select("SELECT * FROM a_model_config WHERE is_active = 1 AND is_deleted = 0 ORDER BY priority ASC, id ASC")
    List<ModelConfig> selectActiveByPriority();

    /**
     * 查询所有启用配置的 AI 调用视图（精简字段），按优先级升序。
     *
     * <p>给 {@link com.aichuangzuo.shared.ai.ModelConfigSelector} 轮询用：只取调用所需字段，
     * 避免把整表实体拖出来。
     */
    @Select("SELECT id, provider_type AS providerType, model_code AS modelCode, "
            + "base_url AS baseUrl, api_key_encrypted AS apiKeyEncrypted "
            + "FROM a_model_config WHERE is_active = 1 AND is_deleted = 0 ORDER BY priority ASC, id ASC")
    List<ActiveModelConfig> selectActiveAiViewByPriority();

    /**
     * 按厂商类型查询未删除的配置列表。
     */
    @Select("SELECT * FROM a_model_config WHERE provider_type = #{providerType} AND is_deleted = 0 ORDER BY priority ASC, id ASC")
    List<ModelConfig> selectByProviderType(@Param("providerType") String providerType);

    /**
     * 检查指定名称在非删除记录中是否已存在（排除自身）。
     */
    @Select("SELECT COUNT(*) FROM a_model_config WHERE name = #{name} AND is_deleted = 0 AND id != #{excludeId}")
    long countByNameExcludingId(@Param("name") String name, @Param("excludeId") Long excludeId);
}
