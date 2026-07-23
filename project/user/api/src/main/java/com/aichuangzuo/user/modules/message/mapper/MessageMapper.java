package com.aichuangzuo.user.modules.message.mapper;

import com.aichuangzuo.user.modules.message.entity.Message;
import com.aichuangzuo.user.modules.message.vo.MessageVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息内容 Mapper。
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 查询用户可见的全部消息，并附带上已读标记，按创建时间倒序，最多返回 200 条。
     * <p>广播消息仅返回用户注册时间之后发布的。</p>
     *
     * @param userId     当前用户ID
     * @param registerAt 用户注册时间
     * @return 消息视图列表
     */
    List<MessageVO> selectVisibleMessages(@Param("userId") Long userId, @Param("registerAt") LocalDateTime registerAt);

    /**
     * 校验某消息对指定用户是否可见。
     *
     * @param messageId  消息ID
     * @param userId     当前用户ID
     * @param registerAt 用户注册时间
     * @return 可见的消息ID，不可见时返回 null
     */
    Long selectVisibleMessageId(@Param("messageId") Long messageId, @Param("userId") Long userId, @Param("registerAt") LocalDateTime registerAt);
}
