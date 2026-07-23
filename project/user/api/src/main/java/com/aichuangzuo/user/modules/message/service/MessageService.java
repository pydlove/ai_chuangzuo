package com.aichuangzuo.user.modules.message.service;

import com.aichuangzuo.user.modules.message.vo.MessageVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户消息中心服务。
 */
public interface MessageService {

    /**
     * 查询当前用户可见的全部消息，附带已读标记。
     * <p>广播消息仅返回用户注册时间之后发布的。</p>
     *
     * @param userId     用户ID
     * @param registerAt 用户注册时间
     * @return 消息列表
     */
    List<MessageVO> listVisibleMessages(Long userId, LocalDateTime registerAt);

    /**
     * 单条标记已读。
     *
     * @param userId     用户ID
     * @param registerAt 用户注册时间
     * @param messageId  消息ID
     */
    void markRead(Long userId, LocalDateTime registerAt, Long messageId);

    /**
     * 全部已读。
     *
     * @param userId     用户ID
     * @param registerAt 用户注册时间
     */
    void markAllRead(Long userId, LocalDateTime registerAt);

    /**
     * 推送一条个人消息，供其他业务模块进程内调用。
     *
     * @param userId   目标用户ID
     * @param msgType  消息类型
     * @param title    标题
     * @param summary  摘要
     * @param linkUrl  跳转路由，可为 null
     * @param content  完整正文，可为 null
     * @param subType  子类型，可为 null
     * @return 消息ID
     */
    Long pushPersonal(Long userId, String msgType, String title, String summary, String linkUrl, String content, String subType);

    /**
     * 发布一条广播消息，供管理端或其他模块进程内调用。
     *
     * @param msgType  消息类型
     * @param title    标题
     * @param summary  摘要
     * @param linkUrl  跳转路由，可为 null
     * @param content  完整正文，可为 null
     * @param subType  子类型，可为 null
     * @return 消息ID
     */
    Long publishBroadcast(String msgType, String title, String summary, String linkUrl, String content, String subType);
}
