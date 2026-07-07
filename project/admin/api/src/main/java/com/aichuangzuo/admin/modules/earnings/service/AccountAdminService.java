package com.aichuangzuo.admin.modules.earnings.service;

import com.aichuangzuo.admin.modules.earnings.dto.request.AccountQueryRequest;
import com.aichuangzuo.admin.modules.earnings.vo.UserAccountDetailVO;
import com.aichuangzuo.admin.modules.earnings.vo.UserAccountPageVO;

public interface AccountAdminService {
    UserAccountPageVO listAccounts(AccountQueryRequest request);
    UserAccountDetailVO getAccountDetail(Long userId);
}
