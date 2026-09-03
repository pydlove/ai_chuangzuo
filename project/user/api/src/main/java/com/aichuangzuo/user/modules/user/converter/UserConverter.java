package com.aichuangzuo.user.modules.user.converter;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * User 实体 → UserProfileVO 的转换器。
 *
 * <p>纯字段映射，不做脱敏、不做默认值填充 —— 这些由调用方按需处理。
 */
@Mapper(componentModel = "spring")
public interface UserConverter {
    /**
     * 将 User 实体映射为视图对象。
     *
     * <p>biz_no（业务编号）映射为 userId；其余字段名一一对应。
     *
     * @param user 实体（已通过 selectById / 鉴权过滤器加载）
     * @return 视图对象；user 为 null 时返回 null（MapStruct 默认行为）
     */
    @Mapping(source = "bizNo", target = "userId")
    @Mapping(target = "inviterUserId", ignore = true)
    @Mapping(target = "inviterNickname", ignore = true)
    @Mapping(target = "inviterAvatarUrl", ignore = true)
    @Mapping(source = "birthday", target = "birthday", dateFormat = "yyyy-MM-dd")
    UserProfileVO toProfileVO(User user);

    /**
     * 兼容旧版头像 URL。
     *
     * <p>早期 storeAvatar 返回 /uploads/avatar/...，线上 Nginx 只代理了 /api/v1/user，
     * 导致旧头像裂图。读数据时自动把旧路径改写为 /api/v1/user/uploads/...，
     * 新路径不受影响。
     */
    @AfterMapping
    default void normalizeAvatarUrl(@MappingTarget UserProfileVO vo) {
        if (vo == null) {
            return;
        }
        String avatarUrl = vo.getAvatarUrl();
        if (avatarUrl != null && avatarUrl.startsWith("/uploads/")) {
            vo.setAvatarUrl("/api/v1/user" + avatarUrl);
        }
    }
}