package com.aichuangzuo.admin.modules.aiprompt.mapper;

import com.aichuangzuo.shared.entity.AiPrompt;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiPromptMapper extends BaseMapper<AiPrompt> {

    /**
     * 按编码查询未删除记录（不校验状态，由业务层处理）。
     */
    @Select("SELECT * FROM c_ai_prompt WHERE prompt_code = #{code} AND is_deleted = 0 LIMIT 1")
    AiPrompt selectByCode(@Param("code") String code);
}
