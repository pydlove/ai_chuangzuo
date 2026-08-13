package com.aichuangzuo.admin.modules.earnings.service;

import com.aichuangzuo.admin.modules.earnings.dto.request.AccountQueryRequest;
import com.aichuangzuo.admin.modules.earnings.dto.request.UserCoinRecordQueryRequest;
import com.aichuangzuo.admin.modules.earnings.dto.request.UserEarningsRecordQueryRequest;
import com.aichuangzuo.admin.modules.earnings.dto.request.UserRewardRecordQueryRequest;
import com.aichuangzuo.admin.modules.earnings.vo.EarningsRecordVO;
import com.aichuangzuo.admin.modules.earnings.vo.PageResult;
import com.aichuangzuo.admin.modules.earnings.vo.RewardRecordVO;
import com.aichuangzuo.admin.modules.earnings.vo.UserAccountDetailVO;
import com.aichuangzuo.admin.modules.earnings.vo.UserAccountPageVO;
import com.aichuangzuo.admin.modules.earnings.vo.UserCoinRecordVO;

public interface AccountAdminService {
    UserAccountPageVO listAccounts(AccountQueryRequest request);

    UserAccountDetailVO getAccountDetail(Long userId);

    PageResult<UserCoinRecordVO> listUserCoinRecords(Long userId, UserCoinRecordQueryRequest request);

    PageResult<EarningsRecordVO> listUserEarningsRecords(Long userId, UserEarningsRecordQueryRequest request);

    PageResult<RewardRecordVO> listUserRewardRecords(Long userId, UserRewardRecordQueryRequest request);
}
