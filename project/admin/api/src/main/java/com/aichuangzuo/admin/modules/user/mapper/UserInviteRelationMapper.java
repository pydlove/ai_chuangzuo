package com.aichuangzuo.admin.modules.user.mapper;

import com.aichuangzuo.admin.modules.user.entity.UserInviteRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户邀请关系表数据访问层（管理端）。
 */
@Mapper
public interface UserInviteRelationMapper extends BaseMapper<UserInviteRelation> {

    /**
     * 根据被邀请人 ID 查询邀请关系。
     *
     * @param inviteeId 被邀请人用户 ID
     * @return 邀请关系；未绑定时返回 null
     */
    @Select("SELECT * FROM u_user_invite_relation WHERE invitee_id = #{inviteeId} LIMIT 1")
    UserInviteRelation selectByInviteeId(@Param("inviteeId") Long inviteeId);

    /**
     * 统计邀请人累计邀请的有效用户数。
     *
     * @param inviterId 邀请人用户 ID
     * @return 有效邀请人数
     */
    @Select("SELECT COUNT(*) FROM u_user_invite_relation WHERE inviter_id = #{inviterId} AND effective_status = 1")
    int countEffectiveByInviterId(@Param("inviterId") Long inviterId);

    /**
     * 查询邀请人邀请的全部用户 ID 列表（按创建时间倒序）。
     *
     * @param inviterId 邀请人用户 ID
     * @return 被邀请人 ID 列表
     */
    @Select("SELECT invitee_id FROM u_user_invite_relation WHERE inviter_id = #{inviterId} ORDER BY created_at DESC")
    List<Long> selectInviteeIdsByInviterId(@Param("inviterId") Long inviterId);
}
