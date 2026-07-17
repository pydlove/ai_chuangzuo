package com.aichuangzuo.admin.modules.exporttemplate.mapper;

import com.aichuangzuo.admin.modules.exporttemplate.entity.ExportTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExportTemplateMapper extends BaseMapper<ExportTemplate> {

    @Select("SELECT * FROM a_export_template WHERE template_key = #{templateKey} AND is_deleted = 0 LIMIT 1")
    ExportTemplate selectByKey(String templateKey);
}
