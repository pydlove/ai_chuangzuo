package com.aichuangzuo.user.modules.earnings.service;

import com.aichuangzuo.user.modules.earnings.dto.request.RealNameRequest;
import com.aichuangzuo.user.modules.earnings.dto.request.WithdrawApplyRequest;
import com.aichuangzuo.user.modules.earnings.dto.request.WithdrawProcessRequest;
import com.aichuangzuo.user.modules.earnings.vo.RealNameVO;
import com.aichuangzuo.user.modules.earnings.vo.WithdrawRequestVO;
import com.aichuangzuo.user.modules.earnings.vo.WithdrawSuccessItemVO;

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
     * 查询全站最近的提现成功记录（用于工作台实时提现动态）。
     *
     * @param limit 返回条数上限
     * @return 提现成功动态（昵称已脱敏）
     */
    List<WithdrawSuccessItemVO> listRecentSuccessfulWithdrawals(int limit);

    /**
     * 申请提现。
     *
     * @param userId  用户ID
     * @param request 提现申请
     * @return 提现业务编号
     */
    String applyWithdraw(Long userId, WithdrawApplyRequest request);

    /**
     * 处理提现申请（管理端）。
     *
     * @param bizNo       提现业务编号
     * @param adminUserId 管理员用户ID
     * @param request     处理请求
     */
    void processWithdraw(String bizNo, Long adminUserId, WithdrawProcessRequest request);
}
