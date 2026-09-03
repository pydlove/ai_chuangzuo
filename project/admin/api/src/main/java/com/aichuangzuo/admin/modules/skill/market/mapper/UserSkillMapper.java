package com.aichuangzuo.admin.modules.skill.market.mapper;

import com.aichuangzuo.admin.modules.skill.entity.UserSkillAggregate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理端 - 提示词市场对用户 skill 表的写操作映射。
 *
 * <p>指向 {@code u_user_skill}，用于管理员代用户发布市场提示词时同步写入发布者的 skill 记录。</p>
 */
@Mapper
public interface UserSkillMapper extends BaseMapper<UserSkillAggregate> {
}
