package com.aichuangzuo.admin.modules.earnings.service.impl;

import com.aichuangzuo.admin.modules.earnings.dto.request.WithdrawQueryRequest;
import com.aichuangzuo.admin.modules.earnings.mapper.WithdrawRequestMapper;
import com.aichuangzuo.admin.modules.earnings.service.WithdrawAdminService;
import com.aichuangzuo.admin.modules.earnings.vo.WithdrawAdminPageVO;
import com.aichuangzuo.admin.modules.earnings.vo.WithdrawAdminVO;
import com.aichuangzuo.admin.modules.leaderboard.client.UserApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawAdminServiceImpl implements WithdrawAdminService {

    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;

    private final WithdrawRequestMapper withdrawRequestMapper;
    private final UserApiClient userApiClient;

    @Override
    public WithdrawAdminPageVO listWithdrawRequests(WithdrawQueryRequest request) {
        long offset = (request.getPage() - 1L) * request.getSize();
        List<WithdrawAdminVO> list = withdrawRequestMapper.selectWithdrawAdminPage(
                request.getUserId(), request.getBizNo(), request.getStatus(), offset, request.getSize());
        for (WithdrawAdminVO vo : list) {
            vo.setStatusText(statusText(vo.getStatus()));
        }
        long total = withdrawRequestMapper.countWithdrawAdminPage(
                request.getUserId(), request.getBizNo(), request.getStatus());

        WithdrawAdminPageVO vo = new WithdrawAdminPageVO();
        vo.setList(list);
        vo.setTotal(total);
        return vo;
    }

    @Override
    public void approve(String bizNo, Long adminUserId) {
        userApiClient.processWithdraw(bizNo, adminUserId, STATUS_APPROVED, null);
        log.info("管理员通过提现申请 bizNo={}, adminUserId={}", bizNo, adminUserId);
    }

    @Override
    public void reject(String bizNo, Long adminUserId, String remark) {
        userApiClient.processWithdraw(bizNo, adminUserId, STATUS_REJECTED, remark);
        log.info("管理员拒绝提现申请 bizNo={}, adminUserId={}", bizNo, adminUserId);
    }

    private String statusText(Integer status) {
        return switch (status) {
            case 1 -> "审核中";
            case 2 -> "已通过";
            case 3 -> "已拒绝";
            default -> "未知";
        };
    }
}
