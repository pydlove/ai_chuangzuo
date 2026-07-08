package com.aichuangzuo.admin.modules.message.vo;

import lombok.Data;

import java.util.List;

@Data
public class MessageAdminPageVO {
    private List<MessageAdminVO> list;
    private long total;
}
