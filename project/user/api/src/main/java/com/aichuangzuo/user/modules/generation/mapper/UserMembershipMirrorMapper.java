package com.aichuangzuo.user.modules.generation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 用户端读 u_user 上的会员字段（admin 端迁移加的两列）：
 * membership_plan / membership_expire_at。
 * 不引入完整镜像实体，节省 mapper 注册。
 */
@Mapper
public interface UserMembershipMirrorMapper {

    @Select("SELECT membership_plan AS planKey, membership_expire_at AS expireAt " +
            "FROM u_user WHERE id = #{userId} AND is_deleted = 0")
    MembershipMirror selectMembership(@Param("userId") Long userId);

    class MembershipMirror {
        private String planKey;
        private LocalDateTime expireAt;
        public String getPlanKey() { return planKey; }
        public void setPlanKey(String planKey) { this.planKey = planKey; }
        public LocalDateTime getExpireAt() { return expireAt; }
        public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    }
}
