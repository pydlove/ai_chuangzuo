package com.aichuangzuo.user.modules.leaderboard.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.entity.IncomeSubmission;
import com.aichuangzuo.user.modules.leaderboard.entity.SubmissionStatus;
import com.aichuangzuo.user.modules.leaderboard.mapper.IncomeSubmissionMapper;
import com.aichuangzuo.user.modules.leaderboard.vo.CoinLeaderboardVO;
import com.aichuangzuo.user.modules.leaderboard.vo.IncomeLeaderboardVO;
import com.aichuangzuo.user.modules.leaderboard.vo.LeaderboardEntryVO;
import com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord;
import com.aichuangzuo.user.modules.leaderboard.entity.CoinDirection;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class LeaderboardServiceTest {

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private CoinRecordService coinRecordService;

    @Autowired
    private UserCoinRecordMapper userCoinRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private IncomeSubmissionMapper incomeSubmissionMapper;

    @Test
    void coinLeaderboard_shouldSortAndMarkCurrentUser() {
        User u1 = createUser("lb-1@test.com");
        User u2 = createUser("lb-2@test.com");
        User u3 = createUser("lb-3@test.com");

        createCoinRecord(u1.getId(), BigDecimal.valueOf(10));
        createCoinRecord(u2.getId(), BigDecimal.valueOf(50));
        createCoinRecord(u3.getId(), BigDecimal.valueOf(30));

        CoinLeaderboardVO vo = leaderboardService.getCoinLeaderboard(u2.getId(), "2026-06");

        assertEquals(3, vo.getTopList().size());
        assertEquals(u2.getId(), vo.getTopList().get(0).getUserId());
        assertEquals(1, vo.getTopList().get(0).getRank());
        assertTrue(vo.getTopList().get(0).getIsMe());
        assertTrue(vo.getMe().getIsMe());
        assertEquals(0, vo.getMe().getAmount().compareTo(BigDecimal.valueOf(50)));
    }

    @Test
    void incomeLeaderboardMonth_shouldOnlyCountApproved() {
        User u1 = createUser("lb-inc-1@test.com");
        User u2 = createUser("lb-inc-2@test.com");

        createSubmission(u1.getId(), "2026-06", BigDecimal.valueOf(1000), SubmissionStatus.APPROVED.getCode());
        createSubmission(u2.getId(), "2026-06", BigDecimal.valueOf(2000), SubmissionStatus.PENDING.getCode());
        createSubmission(u2.getId(), "2026-06", BigDecimal.valueOf(3000), SubmissionStatus.APPROVED.getCode());

        IncomeLeaderboardVO vo = leaderboardService.getIncomeLeaderboard(u1.getId(), "month", "2026-06");

        assertEquals(2, vo.getTopList().size());
        assertEquals(u2.getId(), vo.getTopList().get(0).getUserId());
        assertEquals(0, vo.getTopList().get(0).getAmount().compareTo(BigDecimal.valueOf(3000)));
    }

    private User createUser(String email) {
        User user = new User();
        user.setBizNo("B" + System.nanoTime());
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setNickname(email.split("@")[0]);
        user.setInviteCode("X" + System.nanoTime());
        user.setUserStatus(1);
        user.setEmailVerified(1);
        userMapper.insert(user);
        return user;
    }

    private void createCoinRecord(Long userId, BigDecimal amount) {
        UserCoinRecord record = new UserCoinRecord();
        record.setBizNo("CR" + System.nanoTime());
        record.setUserId(userId);
        record.setBizType("test");
        record.setDirection(CoinDirection.INCOME.getCode());
        record.setAmount(amount);
        record.setBalanceAfter(amount);
        record.setBizTime(LocalDateTime.of(2026, 6, 15, 0, 0));
        record.setTenantId(0L);
        userCoinRecordMapper.insert(record);
    }

    private void createSubmission(Long userId, String month, BigDecimal amount, int status) {
        IncomeSubmission submission = new IncomeSubmission();
        submission.setBizNo("IS" + System.nanoTime());
        submission.setUserId(userId);
        submission.setPeriodMonth(month);
        submission.setAmount(amount);
        submission.setPlatform("xiaohongshu");
        submission.setScreenshotPaths("[]");
        submission.setAuditStatus(status);
        submission.setTenantId(0L);
        incomeSubmissionMapper.insert(submission);
    }
}
