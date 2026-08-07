package com.aichuangzuo.admin.modules.audit.vo;

import lombok.Data;

import java.util.List;

@Data
public class AuditLogPageVO {

    private List<AuditLogVO> list;
    private long total;
    private long page;
    private long pageSize;
}
