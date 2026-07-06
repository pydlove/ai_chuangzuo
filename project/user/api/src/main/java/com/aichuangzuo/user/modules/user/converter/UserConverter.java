package com.aichuangzuo.user.modules.user.converter;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import org.mapstruct.Mapper;

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
     * @param user 实体（已通过 selectById / 鉴权过滤器加载）
     * @return 视图对象；user 为 null 时返回 null（MapStruct 默认行为）
     */
    UserProfileVO toProfileVO(User user);
}