package com.aichuangzuo.user.modules.wechat.service;

import com.aichuangzuo.user.modules.wechat.vo.WechatBindCodeVO;
import com.aichuangzuo.user.modules.wechat.vo.WechatBindQrVO;
import com.aichuangzuo.user.modules.wechat.vo.WechatBindStatusVO;

/**
 * 微信公众号绑定服务。
 */
public interface WechatBindService {

    /**
     * 生成绑定二维码。
     *
     * @param userId 当前登录用户 ID
     * @return 二维码信息
     */
    WechatBindQrVO generateBindQrCode(Long userId);

    /**
     * 生成绑定码。
     *
     * @param userId 当前登录用户 ID
     * @return 绑定码信息
     */
    WechatBindCodeVO generateBindCode(Long userId);

    /**
     * 查询当前用户的绑定状态。
     *
     * @param userId 用户 ID
     * @return 绑定状态
     */
    WechatBindStatusVO checkBindStatus(Long userId);

    /**
     * 根据场景值查询绑定状态。
     *
     * @param sceneStr 场景值
     * @return 绑定状态
     */
    WechatBindStatusVO checkBindStatusBySceneStr(String sceneStr);

    /**
     * 验证服务器配置 URL。
     *
     * @param signature 微信签名
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param echostr   随机字符串
     * @return echostr 或空字符串
     */
    String verifyUrl(String signature, String timestamp, String nonce, String echostr);

    /**
     * 处理微信消息/事件回调。
     *
     * @param signature 微信签名
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param body      XML 消息体
     * @return 回复的 XML
     */
    String handleCallback(String signature, String timestamp, String nonce, String body);
}
