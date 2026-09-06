package com.aichuangzuo.user.modules.wechat.mapper;

import com.aichuangzuo.user.modules.wechat.entity.UserWechatBind;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserWechatBindMapper extends BaseMapper<UserWechatBind> {

    @Select("""
            SELECT id, user_id AS userId, openid, unionid, app_id AS appId,
                   nickname, avatar_url AS avatarUrl, bind_time AS bindTime,
                   create_time AS createTime, update_time AS updateTime
            FROM u_user_wechat_bind
            WHERE openid = #{openid} AND app_id = #{appId}
            LIMIT 1
            """)
    UserWechatBind findByOpenidAndAppId(@Param("openid") String openid, @Param("appId") String appId);

    @Select("""
            SELECT id, user_id AS userId, openid, unionid, app_id AS appId,
                   nickname, avatar_url AS avatarUrl, bind_time AS bindTime,
                   create_time AS createTime, update_time AS updateTime
            FROM u_user_wechat_bind
            WHERE user_id = #{userId} AND app_id = #{appId}
            LIMIT 1
            """)
    UserWechatBind findByUserIdAndAppId(@Param("userId") Long userId, @Param("appId") String appId);
}
