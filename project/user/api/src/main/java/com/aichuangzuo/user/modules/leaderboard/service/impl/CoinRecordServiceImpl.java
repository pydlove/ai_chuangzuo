package com.aichuangzuo.user.modules.leaderboard.service.impl;

import com.aichuangzuo.shared.enums.error.SystemErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CoinRecordServiceImpl implements CoinRecordService {

    private final UserCoinRecordMapper userCoinRecordMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String grant(Long userId, String bizType, BigDecimal amount, String refId, String remark) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(SystemErrorCode.PARAM_VALIDATION_ERROR);
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(SystemErrorCode.PARAM_VALIDATION_ERROR);
        }

        BigDecimal balanceBefore = user.getCoinBalance() == null ? BigDecimal.ZERO : user.getCoinBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        String bizNo = "UC" + System.currentTimeMillis();

        UserCoinRecord record = new UserCoinRecord();
        record.setBizNo(bizNo);
        record.setUserId(userId);
        record.setBizType(bizType);
        record.setDirection(1);
        record.setAmount(amount);
        record.setBalanceAfter(balanceAfter);
        record.setRefId(refId);
        record.setRemark(remark);
        record.setBizTime(LocalDateTime.now());
        record.setTenantId(0L);
        record.setIsDeleted(0);
        record.setCreatedBy(0L);
        record.setUpdatedBy(0L);
        userCoinRecordMapper.insert(record);

        user.setCoinBalance(balanceAfter);
        userMapper.updateById(user);

        return bizNo;
    }
}
