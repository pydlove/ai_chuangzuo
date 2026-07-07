package com.aichuangzuo.user.modules.leaderboard.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.dto.request.IncomeSubmissionUploadRequest;
import com.aichuangzuo.user.modules.leaderboard.entity.SubmissionStatus;
import com.aichuangzuo.user.modules.leaderboard.enums.LeaderboardErrorCode;
import com.aichuangzuo.user.modules.leaderboard.vo.IncomeSubmissionVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class IncomeSubmissionServiceTest {

    @Autowired
    private IncomeSubmissionService incomeSubmissionService;

    @Autowired
    private UserMapper userMapper;

    @Test
    void submit_shouldCreatePendingSubmission() {
        User user = createUser("income-submit@test.com");
        IncomeSubmissionUploadRequest request = new IncomeSubmissionUploadRequest();
        request.setPeriodMonth("2026-06");
        request.setAmount(BigDecimal.valueOf(1234.56));
        request.setPlatform("xiaohongshu");
        request.setScreenshotPaths(List.of("leaderboard/1/Bxxx/1.png"));

        IncomeSubmissionVO vo = incomeSubmissionService.submit(user.getId(), request);

        assertNotNull(vo.getBizNo());
        assertEquals("2026-06", vo.getPeriodMonth());
        assertEquals(0, vo.getAmount().compareTo(BigDecimal.valueOf(1234.56)));
        assertEquals(SubmissionStatus.PENDING.getCode(), vo.getAuditStatus());
        assertFalse(vo.getScreenshotPaths().isEmpty());
    }

    @Test
    void submit_shouldRejectInvalidAmount() {
        User user = createUser("income-invalid@test.com");
        IncomeSubmissionUploadRequest request = new IncomeSubmissionUploadRequest();
        request.setPeriodMonth("2026-06");
        request.setAmount(BigDecimal.ZERO);
        request.setScreenshotPaths(List.of("x"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> incomeSubmissionService.submit(user.getId(), request));
        assertEquals(LeaderboardErrorCode.INCOME_AMOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    void listByUser_shouldReturnOnlyOwnSubmissions() {
        User userA = createUser("income-a@test.com");
        User userB = createUser("income-b@test.com");

        IncomeSubmissionUploadRequest request = new IncomeSubmissionUploadRequest();
        request.setPeriodMonth("2026-06");
        request.setAmount(BigDecimal.TEN);
        request.setScreenshotPaths(List.of("x"));
        incomeSubmissionService.submit(userA.getId(), request);
        incomeSubmissionService.submit(userB.getId(), request);

        List<IncomeSubmissionVO> list = incomeSubmissionService.listByUser(userA.getId(), null);
        assertEquals(1, list.size());
    }

    @Test
    void uploadScreenshots_shouldStoreAndReturnRelativePaths() throws Exception {
        User user = createUser("income-upload@test.com");
        MultipartFile file = new MockMultipartFile(
                "file", "revenue.png", "image/png", "fake-image".getBytes());

        List<String> paths = incomeSubmissionService.uploadScreenshots(user.getId(), List.of(file));

        assertEquals(1, paths.size());
        assertTrue(paths.get(0).startsWith("leaderboard/" + user.getId() + "/"));
        Path stored = Paths.get("data", "uploads").resolve(paths.get(0));
        assertTrue(Files.exists(stored));
        Files.deleteIfExists(stored);
        Files.deleteIfExists(stored.getParent());
        Files.deleteIfExists(stored.getParent().getParent());
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
