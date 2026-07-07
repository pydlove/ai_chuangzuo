package com.aichuangzuo.user.modules.leaderboard.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.entity.CoinDirection;
import com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord;
import com.aichuangzuo.user.modules.leaderboard.enums.LeaderboardErrorCode;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户创作币流水与余额服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoinRecordServiceImpl implements CoinRecordService {

    private static final String BIZ_NO_PREFIX = "CR";

    private final UserCoinRecordMapper coinRecordMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public String grant(Long userId, String bizType, BigDecimal amount, String refId, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("入账金额必须大于 0");
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .setSql("coin_balance = coin_balance + " + amount.toPlainString())
                .eq(User::getId, userId));

        User user = userMapper.selectById(userId);
        BigDecimal balanceAfter = user == null ? amount : user.getCoinBalance();

        UserCoinRecord record = new UserCoinRecord();
        record.setBizNo(generateBizNo());
        record.setUserId(userId);
        record.setBizType(bizType);
        record.setDirection(CoinDirection.INCOME.getCode());
        record.setAmount(amount);
        record.setBalanceAfter(balanceAfter);
        record.setRefId(refId);
        record.setRemark(remark);
        record.setBizTime(LocalDateTime.now());
        record.setTenantId(0L);
        coinRecordMapper.insert(record);

        log.info("创作币入账 userId={}, bizNo={}, amount={}", userId, record.getBizNo(), amount);
        return record.getBizNo();
    }

    @Override
    @Transactional
    public String spend(Long userId, String bizType, BigDecimal amount, String refId, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("扣减金额必须大于 0");
        }

        int affected = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .setSql("coin_balance = coin_balance - " + amount.toPlainString())
                .eq(User::getId, userId)
                .ge(User::getCoinBalance, amount));

        if (affected == 0) {
            throw new BusinessException(LeaderboardErrorCode.COIN_BALANCE_INSUFFICIENT);
        }

        User user = userMapper.selectById(userId);
        BigDecimal balanceAfter = user == null ? BigDecimal.ZERO : user.getCoinBalance();

        UserCoinRecord record = new UserCoinRecord();
        record.setBizNo(generateBizNo());
        record.setUserId(userId);
        record.setBizType(bizType);
        record.setDirection(CoinDirection.EXPENSE.getCode());
        record.setAmount(amount);
        record.setBalanceAfter(balanceAfter);
        record.setRefId(refId);
        record.setRemark(remark);
        record.setBizTime(LocalDateTime.now());
        record.setTenantId(0L);
        coinRecordMapper.insert(record);

        log.info("创作币扣减 userId={}, bizNo={}, amount={}", userId, record.getBizNo(), amount);
        return record.getBizNo();
    }

    @Override
    public BigDecimal getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null || user.getCoinBalance() == null ? BigDecimal.ZERO : user.getCoinBalance();
    }

    private String generateBizNo() {
        return BIZ_NO_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
