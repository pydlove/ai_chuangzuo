package com.aichuangzuo.admin.modules.message.service;

import com.aichuangzuo.admin.modules.message.dto.request.MessageCreateRequest;
import com.aichuangzuo.admin.modules.message.dto.request.MessageQueryRequest;
import com.aichuangzuo.admin.modules.message.dto.request.MessageUpdateRequest;
import com.aichuangzuo.admin.modules.message.vo.MessageAdminPageVO;
import com.aichuangzuo.admin.modules.message.vo.MessageAdminVO;

public interface MessageAdminService {

    MessageAdminPageVO list(MessageQueryRequest request);

    MessageAdminVO detail(Long id);

    MessageAdminVO create(MessageCreateRequest request);

    MessageAdminVO update(Long id, MessageUpdateRequest request);
}
