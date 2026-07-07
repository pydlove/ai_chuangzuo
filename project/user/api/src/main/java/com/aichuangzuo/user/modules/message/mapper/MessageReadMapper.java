package com.aichuangzuo.user.modules.message.mapper;

import com.aichuangzuo.user.modules.message.entity.MessageRead;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户消息已读记录 Mapper。
 */
@Mapper
public interface MessageReadMapper extends BaseMapper<MessageRead> {
}
