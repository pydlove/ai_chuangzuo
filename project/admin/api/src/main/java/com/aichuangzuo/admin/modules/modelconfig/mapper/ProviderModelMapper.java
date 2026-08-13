package com.aichuangzuo.admin.modules.modelconfig.mapper;

import com.aichuangzuo.admin.modules.modelconfig.entity.ProviderModel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProviderModelMapper extends BaseMapper<ProviderModel> {

    /**
     * 查询指定厂商未删除的模型列表。
     */
    @Select("SELECT * FROM a_provider_model WHERE provider_type = #{providerType} AND is_deleted = 0 ORDER BY id ASC")
    List<ProviderModel> selectByProviderType(@Param("providerType") String providerType);
}
