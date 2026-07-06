package com.aichuangzuo.user.modules.auth.mapper;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 按邮箱精确查询未被软删的用户。
     *
     * @param email 邮箱（已规范化为小写）
     * @return 命中的 User；无则 null
     */
    @Select("SELECT * FROM u_user WHERE email = #{email} AND is_deleted = 0 LIMIT 1")
    User selectByEmail(String email);

    /**
     * 按邀请码精确查询未被软删的用户。
     *
     * @param inviteCode 6 位邀请码
     * @return 命中的 User；无则 null
     */
    @Select("SELECT * FROM u_user WHERE invite_code = #{inviteCode} AND is_deleted = 0 LIMIT 1")
    User selectByInviteCode(String inviteCode);

    /**
     * 仅更新密码哈希；不触碰其他字段。
     *
     * @param id   用户主键
     * @param hash 新密码的 BCrypt 哈希
     * @return 受影响行数；通常为 1
     */
    @Update("UPDATE u_user SET password_hash = #{hash}, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updatePassword(@Param("id") Long id, @Param("hash") String hash);

    /**
     * 判断指定邮箱是否已被他人占用（用于改邮箱时校验冲突）。
     *
     * <p>典型调用：{@code existsByEmail(newEmail, currentUserId)} —
     * 如果返回 true，说明新邮箱已被另一个用户注册，不允许改。
     *
     * <p>传 {@code excludeUserId = 0L} 表示不过滤任何用户，等价于"该邮箱是否被任何账号占用"。
     *
     * @param email         待校验的邮箱
     * @param excludeUserId 排除的用户主键（通常是当前登录用户自己）
     * @return true 表示已被他人占用
     */
    @Select("SELECT COUNT(*) > 0 FROM u_user WHERE email = #{email} AND is_deleted = 0 AND id <> #{excludeUserId}")
    boolean existsByEmail(@Param("email") String email, @Param("excludeUserId") Long excludeUserId);
}