package com.aichuangzuo.admin.modules.auth.converter;

import com.aichuangzuo.admin.modules.auth.entity.AdminUser;
import com.aichuangzuo.admin.modules.auth.vo.AdminUserVO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-24T17:08:30+0800",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class AdminAuthConverterImpl implements AdminAuthConverter {

    @Override
    public AdminUserVO toAdminUserVO(AdminUser adminUser) {
        if ( adminUser == null ) {
            return null;
        }

        AdminUserVO adminUserVO = new AdminUserVO();

        adminUserVO.setId( adminUser.getId() );
        adminUserVO.setUsername( adminUser.getUsername() );
        adminUserVO.setRealName( adminUser.getRealName() );
        adminUserVO.setAvatarUrl( adminUser.getAvatarUrl() );

        return adminUserVO;
    }
}
