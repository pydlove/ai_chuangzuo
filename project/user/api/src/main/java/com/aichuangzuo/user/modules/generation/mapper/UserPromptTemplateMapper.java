package com.aichuangzuo.user.modules.generation.mapper;

import com.aichuangzuo.shared.entity.PromptTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户端只读 t_prompt_template（管理端表）：列出已发布模板。
 *
 * <p>设计文档：§5.15.4
 */
@Mapper
public interface UserPromptTemplateMapper {

    @Select("SELECT * FROM t_prompt_template "
            + "WHERE template_status = 1 AND is_deleted = 0 "
            + "ORDER BY id ASC")
    List<PromptTemplate> selectPublished();
}