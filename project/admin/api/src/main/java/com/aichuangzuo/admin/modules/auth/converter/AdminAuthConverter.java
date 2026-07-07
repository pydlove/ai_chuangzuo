package com.aichuangzuo.admin.modules.auth.converter;

import com.aichuangzuo.admin.modules.auth.entity.AdminUser;
import com.aichuangzuo.admin.modules.auth.vo.AdminUserVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdminAuthConverter {

    AdminUserVO toAdminUserVO(AdminUser adminUser);
}
