package com.aichuangzuo.admin.modules.modelconfig.mapper;

import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfig> {

    @Select("SELECT * FROM a_model_config WHERE provider_type = #{providerType} AND is_deleted = 0 LIMIT 1")
    ModelConfig selectByProviderType(@Param("providerType") String providerType);

    @Select("SELECT * FROM a_model_config WHERE provider_type = #{providerType} LIMIT 1")
    ModelConfig selectByProviderTypeIncludingDeleted(@Param("providerType") String providerType);

    @Update("UPDATE a_model_config SET base_url = #{baseUrl}, api_key_encrypted = #{apiKeyEncrypted}, " +
            "model_code = #{modelCode}, model_name = #{modelName}, is_active = #{isActive}, is_deleted = 0, " +
            "updated_at = NOW(3), updated_by = #{updatedBy} WHERE id = #{id}")
    int updateByIdIncludingDeleted(ModelConfig entity);

    @Update("UPDATE a_model_config SET is_deleted = 1, updated_at = NOW(3), updated_by = #{updatedBy} " +
            "WHERE provider_type = #{providerType} AND is_deleted = 0")
    int deleteByProviderType(@Param("providerType") String providerType, @Param("updatedBy") Long updatedBy);
}
