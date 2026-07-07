package com.aichuangzuo.user.modules.auth.mapper;

import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户邀请关系表数据访问层。
 */
@Mapper
public interface UserInviteRelationMapper extends BaseMapper<UserInviteRelation> {

    /**
     * 根据被邀请人 ID 查询邀请关系。
     *
     * <p>用于判断用户是否已绑定邀请人；由于表上有 uk_u_user_invite_relation_invitee_id
     * 唯一约束，结果最多一条。
     *
     * @param inviteeId 被邀请人用户 ID
     * @return 邀请关系；未绑定时返回 null
     */
    @Select("SELECT * FROM u_user_invite_relation WHERE invitee_id = #{inviteeId} LIMIT 1")
    UserInviteRelation selectByInviteeId(@Param("inviteeId") Long inviteeId);
}
