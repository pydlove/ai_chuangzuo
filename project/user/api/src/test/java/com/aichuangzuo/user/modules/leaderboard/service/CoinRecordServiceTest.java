package com.aichuangzuo.user.modules.leaderboard.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.entity.CoinDirection;
import com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord;
import com.aichuangzuo.user.modules.leaderboard.enums.LeaderboardErrorCode;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback
class CoinRecordServiceTest {

    @Autowired
    private CoinRecordService coinRecordService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserCoinRecordMapper coinRecordMapper;

    @Test
    void grant_shouldIncreaseBalanceAndCreateIncomeRecord() {
        User user = createUser("coin-grant@test.com");

        String bizNo = coinRecordService.grant(user.getId(), "test", BigDecimal.TEN, "ref-1", "测试入账");

        assertNotNull(bizNo);
        assertEquals(0, coinRecordService.getBalance(user.getId()).compareTo(BigDecimal.TEN));
        UserCoinRecord record = coinRecordMapper.selectOne(
                new LambdaQueryWrapper<UserCoinRecord>().eq(UserCoinRecord::getBizNo, bizNo));
        assertNotNull(record);
        assertEquals(CoinDirection.INCOME.getCode(), record.getDirection());
        assertEquals(0, record.getAmount().compareTo(BigDecimal.TEN));
    }

    @Test
    void spend_shouldDecreaseBalanceAndCreateExpenseRecord() {
        User user = createUser("coin-spend@test.com");
        coinRecordService.grant(user.getId(), "test", BigDecimal.valueOf(100), null, null);

        String bizNo = coinRecordService.spend(user.getId(), "test", BigDecimal.TEN, "ref-2", "测试扣减");

        assertNotNull(bizNo);
        assertEquals(0, coinRecordService.getBalance(user.getId()).compareTo(BigDecimal.valueOf(90)));
        UserCoinRecord record = coinRecordMapper.selectOne(
                new LambdaQueryWrapper<UserCoinRecord>().eq(UserCoinRecord::getBizNo, bizNo));
        assertNotNull(record);
        assertEquals(CoinDirection.EXPENSE.getCode(), record.getDirection());
    }

    @Test
    void spend_shouldRejectInsufficientBalance() {
        User user = createUser("coin-insufficient@test.com");
        coinRecordService.grant(user.getId(), "test", BigDecimal.TEN, null, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> coinRecordService.spend(user.getId(), "test", BigDecimal.valueOf(11), null, null));
        assertEquals(LeaderboardErrorCode.COIN_BALANCE_INSUFFICIENT.getCode(), ex.getCode());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentGrant_shouldNotOverGrant() throws InterruptedException {
        User user = createUser("coin-concurrent-" + System.nanoTime() + "@test.com");
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    coinRecordService.grant(user.getId(), "test", BigDecimal.TEN, null, "并发入账");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        try {
            assertEquals(0, coinRecordService.getBalance(user.getId()).compareTo(BigDecimal.valueOf(100)));
            assertEquals(threads, coinRecordMapper.selectCount(
                    new LambdaQueryWrapper<UserCoinRecord>().eq(UserCoinRecord::getUserId, user.getId())));
        } finally {
            coinRecordMapper.delete(new LambdaQueryWrapper<UserCoinRecord>().eq(UserCoinRecord::getUserId, user.getId()));
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .set(User::getIsDeleted, 1)
                    .set(User::getUpdatedBy, 0L)
                    .eq(User::getId, user.getId()));
        }
    }

    private User createUser(String email) {
        User user = new User();
        user.setBizNo("B" + System.nanoTime());
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setInviteCode("X" + System.nanoTime());
        user.setUserStatus(1);
        user.setEmailVerified(1);
        userMapper.insert(user);
        return user;
    }
}
