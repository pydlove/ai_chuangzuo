package com.aichuangzuo.admin.modules.skill.preset.mapper;

import com.aichuangzuo.admin.modules.skill.entity.UserSkillAggregate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预设风格 BaseMapper（指向 u_user_skill，提供单行 update / selectOne）。
 */
@Mapper
public interface GlobalSkillMapper extends BaseMapper<UserSkillAggregate> {
}