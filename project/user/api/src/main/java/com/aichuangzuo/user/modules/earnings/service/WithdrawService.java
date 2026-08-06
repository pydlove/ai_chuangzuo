package com.aichuangzuo.user.modules.earnings.service;

import com.aichuangzuo.user.modules.earnings.dto.request.RealNameRequest;
import com.aichuangzuo.user.modules.earnings.dto.request.WithdrawApplyRequest;
import com.aichuangzuo.user.modules.earnings.vo.RealNameVO;
import com.aichuangzuo.user.modules.earnings.vo.WithdrawRequestVO;

import java.util.List;

/**
 * 用户实名认证与提现服务。
 */
public interface WithdrawService {

    /**
     * 获取用户实名信息。
     *
     * @param userId 用户ID
     * @return 实名信息，未认证返回 null
     */
    RealNameVO getRealName(Long userId);

    /**
     * 提交实名认证。
     *
     * @param userId  用户ID
     * @param request 实名信息
     */
    void submitRealName(Long userId, RealNameRequest request);

    /**
     * 查询用户提现记录列表（按时间倒序）。
     *
     * @param userId 用户ID
     * @return 提现记录
     */
    List<WithdrawRequestVO> listWithdrawRequests(Long userId);

    /**
     * 申请提现。
     *
     * @param userId  用户ID
     * @param request 提现申请
     * @return 提现业务编号
     */
    String applyWithdraw(Long userId, WithdrawApplyRequest request);
}
