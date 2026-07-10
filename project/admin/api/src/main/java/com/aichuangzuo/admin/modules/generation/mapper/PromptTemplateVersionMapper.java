package com.aichuangzuo.admin.modules.generation.mapper;

import com.aichuangzuo.shared.entity.PromptTemplateVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PromptTemplateVersionMapper extends BaseMapper<PromptTemplateVersion> {

    /**
     * 取某模板全部版本快照，按 version 降序。
     */
    List<PromptTemplateVersion> selectByTemplateId(@Param("templateId") Long templateId);

    /**
     * 取某模板最新已发布版本（version_status=1）。无已发布版本返回 null。
     */
    PromptTemplateVersion selectLatestPublished(@Param("templateId") Long templateId);
}