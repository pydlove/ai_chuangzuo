package com.aichuangzuo.admin.modules.earnings.service;

import com.aichuangzuo.admin.modules.earnings.dto.request.SettlementRequest;
import com.aichuangzuo.admin.modules.earnings.vo.PendingSettlementSummaryVO;
import com.aichuangzuo.admin.modules.earnings.vo.PendingSettlementUserVO;
import com.aichuangzuo.admin.modules.earnings.vo.SettlementResultVO;

import java.util.List;

public interface SettlementAdminService {
    PendingSettlementSummaryVO pendingSummary(String month);
    List<PendingSettlementUserVO> pendingUsers(String month);
    SettlementResultVO settle(SettlementRequest request);
}
