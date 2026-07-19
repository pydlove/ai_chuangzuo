package com.aichuangzuo.admin.infrastructure.persistence.handler;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        Long currentUser = currentUserIdOrZero();
        this.strictInsertFill(metaObject, "createdBy", Long.class, currentUser);
        this.strictInsertFill(metaObject, "updatedBy", Long.class, currentUser);
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, currentUserIdOrZero());
    }

    private static Long currentUserIdOrZero() {
        Long uid = SecurityAdminContext.getCurrentAdminUserId();
        return uid != null ? uid : 0L;
    }
}
