package com.aichuangzuo.admin.modules.order.mapper;

import com.aichuangzuo.admin.modules.order.entity.AdminMembership;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AdminMembershipMapper {

    AdminMembership selectByUserId(@Param("userId") Long userId);

    int insertMembership(AdminMembership membership);

    int updateMembership(AdminMembership membership);

    int updateUserMembershipFields(@Param("userId") Long userId,
                                   @Param("expireAt") LocalDateTime expireAt,
                                   @Param("plan") String plan);

    int userExists(@Param("userId") Long userId);
}
