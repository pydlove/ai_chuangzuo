package com.aichuangzuo.user.modules.wechat.mapper;

import com.aichuangzuo.user.modules.wechat.entity.WechatBindCode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WechatBindCodeMapper extends BaseMapper<WechatBindCode> {

    @Select("""
            SELECT id, user_id AS userId, bind_code AS bindCode, openid, app_id AS appId,
                   status, expire_time AS expireTime, create_time AS createTime, update_time AS updateTime
            FROM u_wechat_bind_code
            WHERE bind_code = #{bindCode}
            LIMIT 1
            """)
    WechatBindCode findByBindCode(@Param("bindCode") String bindCode);

    @Update("""
            UPDATE u_wechat_bind_code
            SET status = #{status},
                openid = #{openid},
                update_time = CURRENT_TIMESTAMP(3)
            WHERE bind_code = #{bindCode}
            """)
    int updateStatusByBindCode(@Param("bindCode") String bindCode,
                               @Param("status") Integer status,
                               @Param("openid") String openid);
}
