package com.aichuangzuo.user.modules.aiprompt.mapper;

import com.aichuangzuo.shared.entity.AiPrompt;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiPromptMapper extends BaseMapper<AiPrompt> {

    @Select("SELECT * FROM c_ai_prompt WHERE prompt_code = #{code} AND status = 1 AND is_deleted = 0 LIMIT 1")
    AiPrompt selectActiveByCode(@Param("code") String code);
}
