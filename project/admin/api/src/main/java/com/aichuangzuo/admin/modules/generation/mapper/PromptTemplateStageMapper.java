package com.aichuangzuo.admin.modules.generation.mapper;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PromptTemplateStageMapper extends BaseMapper<PromptTemplateStage> {

    /**
     * 取某个模板的全部 stage（按 stage_index 升序）。
     */
    List<PromptTemplateStage> selectByTemplateId(@Param("templateId") Long templateId);

    /**
     * 物理删除某模板的全部 stage（事务里配合批量 insert 实现「全量替换」）。
     */
    int deleteByTemplateId(@Param("templateId") Long templateId);
}
