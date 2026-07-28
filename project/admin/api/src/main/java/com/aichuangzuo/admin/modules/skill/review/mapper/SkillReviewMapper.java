package com.aichuangzuo.admin.modules.skill.review.mapper;

import com.aichuangzuo.admin.modules.skill.entity.UserSkillAggregate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风格审核 BaseMapper，提供单行更新与简单查询。
 */
@Mapper
public interface SkillReviewMapper extends BaseMapper<UserSkillAggregate> {
}