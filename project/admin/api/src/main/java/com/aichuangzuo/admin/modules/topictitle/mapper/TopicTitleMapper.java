package com.aichuangzuo.admin.modules.topictitle.mapper;

import com.aichuangzuo.shared.entity.TopicTitle;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 选题标题库 Mapper（管理端）：分页 / 入库 / 逻辑删除均走 BaseMapper。
 */
@Mapper
public interface TopicTitleMapper extends BaseMapper<TopicTitle> {
}
