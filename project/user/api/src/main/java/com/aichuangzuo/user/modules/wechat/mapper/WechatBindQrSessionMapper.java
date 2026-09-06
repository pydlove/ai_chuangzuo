package com.aichuangzuo.user.modules.wechat.mapper;

import com.aichuangzuo.user.modules.wechat.entity.WechatBindQrSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface WechatBindQrSessionMapper extends BaseMapper<WechatBindQrSession> {

    @Select("""
            SELECT id, scene_str AS sceneStr, user_id AS userId, status, openid,
                   expire_time AS expireTime, create_time AS createTime, update_time AS updateTime
            FROM u_wechat_bind_qr_session
            WHERE scene_str = #{sceneStr}
            LIMIT 1
            """)
    WechatBindQrSession findBySceneStr(@Param("sceneStr") String sceneStr);

    @Select("""
            SELECT id, scene_str AS sceneStr, user_id AS userId, status, openid,
                   expire_time AS expireTime, create_time AS createTime, update_time AS updateTime
            FROM u_wechat_bind_qr_session
            WHERE user_id = #{userId}
            ORDER BY id DESC
            LIMIT 1
            """)
    WechatBindQrSession findLatestByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE u_wechat_bind_qr_session
            SET status = #{status},
                openid = #{openid},
                update_time = CURRENT_TIMESTAMP(3)
            WHERE scene_str = #{sceneStr}
            """)
    int updateStatusBySceneStr(@Param("sceneStr") String sceneStr,
                               @Param("status") Integer status,
                               @Param("openid") String openid);

    @Update("""
            UPDATE u_wechat_bind_qr_session
            SET status = -1,
                update_time = CURRENT_TIMESTAMP(3)
            WHERE expire_time < #{now} AND status = 0
            """)
    int markExpired(@Param("now") LocalDateTime now);
}
