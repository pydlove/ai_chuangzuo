package com.aichuangzuo.admin.modules.earnings.service;

import com.aichuangzuo.admin.modules.earnings.dto.request.WithdrawQueryRequest;
import com.aichuangzuo.admin.modules.earnings.vo.WithdrawAdminPageVO;

public interface WithdrawAdminService {

    WithdrawAdminPageVO listWithdrawRequests(WithdrawQueryRequest request);

    void approve(String bizNo, Long adminUserId);

    void reject(String bizNo, Long adminUserId, String remark);
}
