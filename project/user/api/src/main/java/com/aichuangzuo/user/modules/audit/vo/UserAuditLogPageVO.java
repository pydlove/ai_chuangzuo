package com.aichuangzuo.user.modules.audit.vo;

import com.aichuangzuo.user.modules.audit.entity.UserAuditLog;
import lombok.Data;

import java.util.List;

@Data
public class UserAuditLogPageVO {

    private List<UserAuditLog> list;

    private long total;

    private long page;

    private long pageSize;
}
