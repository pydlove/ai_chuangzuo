package com.aichuangzuo.admin.modules.generation.mapper;

import com.aichuangzuo.shared.entity.PromptTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {

    /**
     * 取所有已发布（template_status=1）且未删除的模板，按 id 升序。
     * 阶段 3 user 端只读接口使用。
     */
    @Select("SELECT * FROM t_prompt_template WHERE template_status = 1 AND is_deleted = 0 ORDER BY id ASC")
    List<PromptTemplate> selectPublished();
}
