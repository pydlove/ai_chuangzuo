package com.aichuangzuo.admin.modules.style.preset.mapper;

import com.aichuangzuo.admin.modules.style.entity.UserStyleAggregate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预设风格 BaseMapper（指向 u_user_style，提供单行 update / selectOne）。
 */
@Mapper
public interface GlobalStyleMapper extends BaseMapper<UserStyleAggregate> {
}