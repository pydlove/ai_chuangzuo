package com.aichuangzuo.admin.modules.user.service;

import com.aichuangzuo.admin.modules.benefit.entity.BenefitUsageAggregate;
import com.aichuangzuo.admin.modules.benefit.mapper.BenefitUsageAdminMapper;
import com.aichuangzuo.admin.modules.order.entity.AdminMembership;
import com.aichuangzuo.admin.modules.order.mapper.AdminMembershipMapper;
import com.aichuangzuo.admin.modules.plan.mapper.PlanMapper;
import com.aichuangzuo.admin.modules.planbenefit.entity.PlanBenefit;
import com.aichuangzuo.admin.modules.planbenefit.mapper.PlanBenefitMapper;
import com.aichuangzuo.admin.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.admin.modules.skill.market.entity.UserMarketFavorite;
import com.aichuangzuo.admin.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.admin.modules.skill.market.mapper.UserMarketFavoriteMapper;
import com.aichuangzuo.admin.modules.user.dto.excel.UserImportExcelRowData;
import com.aichuangzuo.admin.modules.earnings.entity.UserCoinRecord;
import com.aichuangzuo.admin.modules.earnings.mapper.UserCoinRecordMapper;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserCreateRequest;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserUpdateRequest;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.entity.UserInviteRelation;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserLoginLogMapper;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.admin.modules.user.mapper.UserInviteRelationMapper;
import com.aichuangzuo.admin.modules.user.service.impl.AdminUserServiceImpl;
import com.aichuangzuo.admin.modules.user.vo.AdminUserFavoriteSkillVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserImportResultVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserInviteDetailVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPublishedSkillVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private PlatformUserMapper platformUserMapper;

    @Mock
    private PlatformUserLoginLogMapper platformUserLoginLogMapper;

    @Mock
    private UserInviteRelationMapper userInviteRelationMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SkillMarketMapper skillMarketMapper;

    @Mock
    private UserMarketFavoriteMapper userMarketFavoriteMapper;

    @Mock
    private PlanMapper planMapper;

    @Mock
    private PlanBenefitMapper planBenefitMapper;

    @Mock
    private AdminMembershipMapper adminMembershipMapper;

    @Mock
    private BenefitUsageAdminMapper benefitUsageAdminMapper;

    @Mock
    private UserCoinRecordMapper userCoinRecordMapper;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(userInviteRelationMapper.countEffectiveByInviterId(any())).thenReturn(0);
        lenient().when(userInviteRelationMapper.selectByInviteeId(any())).thenReturn(null);
    }

    @Test
    void listUsers_shouldReturnPage() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setNickname("test");
        user.setInviteCode("ABC123");
        user.setUserStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setIsDeleted(0);

        Page<PlatformUser> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(user));
        page.setTotal(1);

        when(platformUserMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(platformUserLoginLogMapper.selectLastLoginAtByUserId(1L)).thenReturn(LocalDateTime.now());

        AdminUserPageVO result = adminUserService.listUsers("", null, 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("user@example.com", result.getList().get(0).getAccount());
        assertEquals("enabled", result.getList().get(0).getStatus());
    }

    @Test
    void listUsers_withInviteCode_shouldFilterByExactInviteCode() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setNickname("test");
        user.setInviteCode("ABC123");
        user.setUserStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setIsDeleted(0);

        Page<PlatformUser> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(user));
        page.setTotal(1);

        when(platformUserMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(platformUserLoginLogMapper.selectLastLoginAtByUserId(1L)).thenReturn(LocalDateTime.now());

        AdminUserPageVO result = adminUserService.listUsers(null, "ABC123", 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals("ABC123", result.getList().get(0).getInviteCode());

        ArgumentCaptor<LambdaQueryWrapper<PlatformUser>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(platformUserMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<PlatformUser> captured = wrapperCaptor.getValue();
        assertNotNull(captured);
    }

    @Test
    void getUser_shouldThrowWhenNotFound() {
        when(platformUserMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.getUser(999L));
        assertEquals(AdminUserErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void getUser_withActiveMembership_shouldCalculateRemainingArticleQuota() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setNickname("test");
        user.setUserStatus(1);
        user.setUserType(1);
        user.setIsDeleted(0);

        AdminMembership membership = new AdminMembership();
        membership.setUserId(1L);
        membership.setLevel("pro");
        membership.setExpiresAt(LocalDate.now().plusDays(10));

        PlanBenefit planBenefit = new PlanBenefit();
        planBenefit.setPlanKey("pro");
        planBenefit.setBenefitCode("ai_article_quota");
        planBenefit.setBenefitValue("100");

        BenefitUsageAggregate usage = new BenefitUsageAggregate();
        usage.setUserId(1L);
        usage.setBenefitCode("ai_article_quota");
        usage.setPeriod(YearMonth.now().toString());
        usage.setUsedCount(30);
        usage.setPreUsedCount(5);

        when(platformUserMapper.selectById(1L)).thenReturn(user);
        when(adminMembershipMapper.selectByUserId(1L)).thenReturn(membership);
        when(planBenefitMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(planBenefit);
        when(benefitUsageAdminMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(usage);
        when(platformUserLoginLogMapper.selectLastLoginAtByUserId(1L)).thenReturn(LocalDateTime.now());

        AdminUserVO result = adminUserService.getUser(1L);

        assertEquals(65, result.getRemainingArticleQuota());
    }

    @Test
    void getUser_withoutMembership_shouldReturnZeroRemainingArticleQuota() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("free@example.com");
        user.setNickname("test");
        user.setUserStatus(1);
        user.setUserType(1);
        user.setIsDeleted(0);

        when(platformUserMapper.selectById(1L)).thenReturn(user);
        when(adminMembershipMapper.selectByUserId(1L)).thenReturn(null);
        when(platformUserLoginLogMapper.selectLastLoginAtByUserId(1L)).thenReturn(LocalDateTime.now());

        AdminUserVO result = adminUserService.getUser(1L);

        assertEquals(0, result.getRemainingArticleQuota());
    }

    @Test
    void updateUser_phoneOnly_shouldSucceed() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("old@example.com");
        user.setPhone("13800138000");
        user.setNickname("旧用户");
        user.setUserStatus(1);
        user.setUserType(1);
        user.setIsDeleted(0);

        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setEmail("");
        request.setPhone("13900139000");
        request.setNickname("新用户");
        request.setStatus("enabled");
        request.setUserType(1);

        when(platformUserMapper.selectById(1L)).thenReturn(user);
        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(platformUserLoginLogMapper.selectLastLoginAtByUserId(1L)).thenReturn(LocalDateTime.now());
        when(adminMembershipMapper.selectByUserId(1L)).thenReturn(null);

        AdminUserVO result = adminUserService.updateUser(1L, request);

        assertEquals("新用户", result.getNickname());
        assertNull(result.getEmail());
        assertEquals("13900139000", result.getPhone());
        verify(platformUserMapper).updateById(user);
    }

    @Test
    void updateUser_withoutEmailNorPhone_shouldThrow() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("old@example.com");
        user.setNickname("旧用户");
        user.setUserStatus(1);
        user.setUserType(1);
        user.setIsDeleted(0);

        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setEmail("");
        request.setPhone("");
        request.setNickname("新用户");
        request.setStatus("enabled");
        request.setUserType(1);

        when(platformUserMapper.selectById(1L)).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.updateUser(1L, request));
        assertEquals(AdminUserErrorCode.PHONE_OR_EMAIL_REQUIRED.getCode(), ex.getCode());
        verify(platformUserMapper, never()).updateById(any(PlatformUser.class));
    }

    @Test
    void updateUser_withMonthlyCoinEarnings_shouldInsertRecordAndUpdateBalance() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("old@example.com");
        user.setNickname("旧用户");
        user.setUserStatus(1);
        user.setUserType(1);
        user.setCoinBalance(new BigDecimal("100"));
        user.setIsDeleted(0);

        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setEmail("old@example.com");
        request.setNickname("新用户");
        request.setStatus("enabled");
        request.setUserType(1);
        request.setMonthlyCoinEarnings(new BigDecimal("500"));

        when(platformUserMapper.selectById(1L)).thenReturn(user);
        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(platformUserLoginLogMapper.selectLastLoginAtByUserId(1L)).thenReturn(LocalDateTime.now());
        when(adminMembershipMapper.selectByUserId(1L)).thenReturn(null);
        when(userCoinRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

        adminUserService.updateUser(1L, request);

        verify(platformUserMapper).updateById(user);
        ArgumentCaptor<UserCoinRecord> captor = ArgumentCaptor.forClass(UserCoinRecord.class);
        verify(userCoinRecordMapper).insert(captor.capture());
        UserCoinRecord record = captor.getValue();
        assertEquals(new BigDecimal("500"), record.getAmount());
        assertEquals(Integer.valueOf(1), record.getDirection());
        assertEquals("admin_monthly_coin_earnings", record.getBizType());
    }

    @Test
    void updateUser_withZeroMonthlyCoinEarnings_shouldClearExistingAndNotInsert() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("old@example.com");
        user.setNickname("旧用户");
        user.setUserStatus(1);
        user.setUserType(1);
        user.setCoinBalance(new BigDecimal("300"));
        user.setIsDeleted(0);

        UserCoinRecord existing = new UserCoinRecord();
        existing.setUserId(1L);
        existing.setBizType("admin_monthly_coin_earnings");
        existing.setAmount(new BigDecimal("200"));
        existing.setDirection(1);

        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setEmail("old@example.com");
        request.setNickname("新用户");
        request.setStatus("enabled");
        request.setUserType(1);
        request.setMonthlyCoinEarnings(BigDecimal.ZERO);

        when(platformUserMapper.selectById(1L)).thenReturn(user);
        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(platformUserLoginLogMapper.selectLastLoginAtByUserId(1L)).thenReturn(LocalDateTime.now());
        when(adminMembershipMapper.selectByUserId(1L)).thenReturn(null);
        when(userCoinRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.singletonList(existing));

        adminUserService.updateUser(1L, request);

        verify(userCoinRecordMapper).delete(any(QueryWrapper.class));
        verify(userCoinRecordMapper, never()).insert(any(UserCoinRecord.class));
    }

    @Test
    void updateUser_withNegativeMonthlyCoinEarnings_shouldThrow() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("old@example.com");
        user.setNickname("旧用户");
        user.setUserStatus(1);
        user.setUserType(1);
        user.setIsDeleted(0);

        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setEmail("old@example.com");
        request.setNickname("新用户");
        request.setStatus("enabled");
        request.setUserType(1);
        request.setMonthlyCoinEarnings(new BigDecimal("-10"));

        when(platformUserMapper.selectById(1L)).thenReturn(user);
        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.updateUser(1L, request));
        assertEquals(AdminUserErrorCode.MONTHLY_COIN_EARNINGS_INVALID.getCode(), ex.getCode());
        verify(userCoinRecordMapper, never()).insert(any(UserCoinRecord.class));
    }

    @Test
    void resetPassword_shouldReturnFixedPassword() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setIsDeleted(0);
        when(platformUserMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.encode("Aichuangzuo@123")).thenReturn("hashed");

        AdminUserResetPasswordVO result = adminUserService.resetPassword(1L);

        assertEquals("Aichuangzuo@123", result.getNewPassword());
        verify(platformUserMapper).updateById(user);
        assertEquals("hashed", user.getPasswordHash());
    }

    @Test
    void createUser_realUser_shouldInsertWithDefaults() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("new@example.com");
        request.setNickname("新用户");
        request.setStatus("enabled");
        request.setUserType(1);

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("Aichuangzuo@123")).thenReturn("hashed");

        AdminUserVO result = adminUserService.createUser(request);

        assertEquals("new@example.com", result.getEmail());
        assertEquals("新用户", result.getNickname());
        assertEquals("real", result.getUserType());
        assertEquals("enabled", result.getStatus());

        ArgumentCaptor<PlatformUser> captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(platformUserMapper).insert(captor.capture());
        PlatformUser saved = captor.getValue();
        assertNotNull(saved.getBizNo());
        assertTrue(saved.getBizNo().startsWith("U"));
        assertEquals("hashed", saved.getPasswordHash());
        assertEquals(1, saved.getUserStatus());
        assertEquals(1, saved.getUserType());
        assertEquals(1, saved.getEmailVerified());
        assertNotNull(saved.getInviteCode());
        assertEquals(6, saved.getInviteCode().length());
    }

    @Test
    void createUser_robotUser_shouldMarkRobot() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("robot@example.com");
        request.setNickname("机器人");
        request.setStatus("enabled");
        request.setUserType(0);

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("Aichuangzuo@123")).thenReturn("hashed");

        AdminUserVO result = adminUserService.createUser(request);

        assertEquals("robot", result.getUserType());
        ArgumentCaptor<PlatformUser> captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(platformUserMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getUserType());
    }

    @Test
    void createUser_duplicateEmail_shouldThrow() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("dup@example.com");
        request.setNickname("重复");
        request.setStatus("enabled");
        request.setUserType(1);

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.createUser(request));
        assertEquals(AdminUserErrorCode.EMAIL_ALREADY_EXISTS.getCode(), ex.getCode());
        verify(platformUserMapper, never()).insert(any(PlatformUser.class));
    }

    @Test
    void createUser_invalidUserType_shouldThrow() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("type@example.com");
        request.setNickname("类型错误");
        request.setStatus("enabled");
        request.setUserType(2);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.createUser(request));
        assertEquals(AdminUserErrorCode.USER_TYPE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void createUser_customPassword_shouldUseProvidedPassword() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("custom@example.com");
        request.setNickname("自定义密码");
        request.setPassword("mySecret123");
        request.setStatus("enabled");
        request.setUserType(1);

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("mySecret123")).thenReturn("customHash");

        adminUserService.createUser(request);

        ArgumentCaptor<PlatformUser> captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(platformUserMapper).insert(captor.capture());
        assertEquals("customHash", captor.getValue().getPasswordHash());
    }

    @Test
    void createUser_phoneOnly_shouldSucceed() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setPhone("13800138000");
        request.setNickname("手机用户");
        request.setStatus("enabled");
        request.setUserType(1);

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("Aichuangzuo@123")).thenReturn("hashed");

        AdminUserVO result = adminUserService.createUser(request);

        assertEquals("13800138000", result.getPhone());
        assertNull(result.getEmail());
        assertEquals("enabled", result.getStatus());

        ArgumentCaptor<PlatformUser> captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(platformUserMapper).insert(captor.capture());
        PlatformUser saved = captor.getValue();
        assertEquals("13800138000", saved.getPhone());
        assertNull(saved.getEmail());
        assertEquals(1, saved.getPhoneVerified());
        assertEquals(0, saved.getEmailVerified());
    }

    @Test
    void createUser_withoutEmailNorPhone_shouldThrow() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setNickname("无账号");
        request.setStatus("enabled");
        request.setUserType(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.createUser(request));
        assertEquals(AdminUserErrorCode.PHONE_OR_EMAIL_REQUIRED.getCode(), ex.getCode());
        verify(platformUserMapper, never()).insert(any(PlatformUser.class));
    }

    @Test
    void createUser_disabledStatus_shouldSucceed() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("disabled@example.com");
        request.setNickname("禁用用户");
        request.setStatus("disabled");
        request.setUserType(1);

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("Aichuangzuo@123")).thenReturn("hashed");

        AdminUserVO result = adminUserService.createUser(request);

        assertEquals("disabled", result.getStatus());
        ArgumentCaptor<PlatformUser> captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(platformUserMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getUserStatus());
    }

    @Test
    void createUser_withMembership_shouldSyncMembership() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("member@example.com");
        request.setNickname("会员用户");
        request.setStatus("enabled");
        request.setUserType(1);
        request.setMembershipPlan("pro");
        request.setExpireDate(LocalDate.now().plusDays(10));

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("Aichuangzuo@123")).thenReturn("hashed");
        when(planMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(adminMembershipMapper.selectByUserId(any())).thenReturn(null);

        AdminUserVO result = adminUserService.createUser(request);

        assertEquals("pro", result.getMembershipPlan());
        ArgumentCaptor<PlatformUser> captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(platformUserMapper).insert(captor.capture());
        PlatformUser saved = captor.getValue();
        assertEquals("pro", saved.getMembershipPlan());
        assertNotNull(saved.getMembershipExpireAt());
        verify(adminMembershipMapper).insertMembership(any(AdminMembership.class));
    }

    @Test
    void createUser_withMonthlyCoinEarnings_shouldInsertRecord() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setEmail("earnings@example.com");
        request.setNickname("收益用户");
        request.setStatus("enabled");
        request.setUserType(1);
        request.setMonthlyCoinEarnings(new BigDecimal("500"));

        PlatformUser current = new PlatformUser();
        current.setId(1L);
        current.setCoinBalance(new BigDecimal("100"));

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("Aichuangzuo@123")).thenReturn("hashed");
        when(userCoinRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        when(platformUserMapper.selectById(any())).thenReturn(current);

        adminUserService.createUser(request);

        ArgumentCaptor<UserCoinRecord> captor = ArgumentCaptor.forClass(UserCoinRecord.class);
        verify(userCoinRecordMapper).insert(captor.capture());
        UserCoinRecord record = captor.getValue();
        assertEquals(new BigDecimal("500"), record.getAmount());
        assertEquals(Integer.valueOf(1), record.getDirection());
        assertEquals("admin_monthly_coin_earnings", record.getBizType());
    }

    @Test
    void getUserInviteDetail_shouldReturnInviterAndInvitees() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setInviteCode("ABC123");
        user.setIsDeleted(0);

        PlatformUser inviter = new PlatformUser();
        inviter.setId(2L);
        inviter.setEmail("inviter@example.com");
        inviter.setNickname("邀请人");
        inviter.setIsDeleted(0);

        UserInviteRelation relation = new UserInviteRelation();
        relation.setInviterId(2L);
        relation.setInviteeId(1L);
        relation.setCreatedAt(LocalDateTime.now());

        PlatformUser invitee = new PlatformUser();
        invitee.setId(3L);
        invitee.setEmail("invitee@example.com");
        invitee.setNickname("被邀请人");
        invitee.setIsDeleted(0);

        when(platformUserMapper.selectById(1L)).thenReturn(user);
        when(userInviteRelationMapper.selectByInviteeId(1L)).thenReturn(relation);
        when(platformUserMapper.selectById(2L)).thenReturn(inviter);
        when(userInviteRelationMapper.countEffectiveByInviterId(1L)).thenReturn(1);
        when(userInviteRelationMapper.selectEffectiveInviteeIdsByInviterId(1L, 0, 10)).thenReturn(Arrays.asList(3L));
        when(platformUserMapper.selectBatchIds(Arrays.asList(3L))).thenReturn(Arrays.asList(invitee));

        AdminUserInviteDetailVO result = adminUserService.getUserInviteDetail(1L, 1, 10);

        assertEquals("ABC123", result.getInviteCode());
        assertNotNull(result.getInviter());
        assertEquals("inviter@example.com", result.getInviter().getEmail());
        assertEquals(1, result.getInvitees().size());
        assertEquals("invitee@example.com", result.getInvitees().get(0).getEmail());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void listUserFavoriteSkills_shouldReturnFavoriteSkillsWithPublisher() {
        PlatformUser user = new PlatformUser();
        user.setId(1L);
        user.setIsDeleted(0);

        UserMarketFavorite favorite = new UserMarketFavorite();
        favorite.setUserId(1L);
        favorite.setMarketSkillId("S0001");
        favorite.setCreatedAt(LocalDateTime.of(2026, 8, 1, 10, 0, 0));

        SkillMarket market = new SkillMarket();
        market.setBizNo("S0001");
        market.setSkillName("收藏提示词");
        market.setPromptSummary("摘要");
        market.setPrompt("提示词内容");
        market.setScope("小红书");
        market.setPublisherUserId(2L);
        market.setAuditStatus(1);

        PlatformUser publisher = new PlatformUser();
        publisher.setId(2L);
        publisher.setEmail("publisher@example.com");
        publisher.setNickname("发布者");
        publisher.setIsDeleted(0);

        when(platformUserMapper.selectById(1L)).thenReturn(user);
        when(userMarketFavoriteMapper.selectList(any())).thenReturn(Arrays.asList(favorite));
        when(skillMarketMapper.selectList(any())).thenReturn(Arrays.asList(market));
        when(platformUserMapper.selectBatchIds(Arrays.asList(2L))).thenReturn(Arrays.asList(publisher));

        List<AdminUserFavoriteSkillVO> result = adminUserService.listUserFavoriteSkills(1L);

        assertEquals(1, result.size());
        assertEquals("S0001", result.get(0).getBizNo());
        assertEquals("收藏提示词", result.get(0).getSkillName());
        assertEquals("publisher@example.com", result.get(0).getPublisherEmail());
        assertEquals("发布者", result.get(0).getPublisherNickname());
        assertNotNull(result.get(0).getFavoriteAt());
    }

    @Test
    void importUsersFromExcel_phoneOnly_shouldSucceed() throws IOException {
        UserImportExcelRowData row = new UserImportExcelRowData();
        row.setPhone("13800138000");
        row.setNickname("手机用户");
        row.setPassword("");
        row.setUserType("1");

        MultipartFile file = createExcelFile(List.of(row));

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("Aichuangzuo@123")).thenReturn("hashed");

        AdminUserImportResultVO result = adminUserService.importUsersFromExcel(file);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getImportedCount());

        ArgumentCaptor<PlatformUser> captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(platformUserMapper).insert(captor.capture());
        PlatformUser saved = captor.getValue();
        assertEquals("13800138000", saved.getPhone());
        assertNull(saved.getEmail());
        assertEquals("手机用户", saved.getNickname());
        assertEquals(1, saved.getPhoneVerified());
        assertEquals(0, saved.getEmailVerified());
    }

    @Test
    void importUsersFromExcel_bothEmailAndPhone_shouldSucceed() throws IOException {
        UserImportExcelRowData row = new UserImportExcelRowData();
        row.setEmail("both@example.com");
        row.setPhone("13900139000");
        row.setNickname("双账号用户");
        row.setPassword("");
        row.setUserType("0");

        MultipartFile file = createExcelFile(List.of(row));

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("Aichuangzuo@123")).thenReturn("hashed");

        AdminUserImportResultVO result = adminUserService.importUsersFromExcel(file);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getImportedCount());

        ArgumentCaptor<PlatformUser> captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(platformUserMapper).insert(captor.capture());
        PlatformUser saved = captor.getValue();
        assertEquals("both@example.com", saved.getEmail());
        assertEquals("13900139000", saved.getPhone());
        assertEquals(1, saved.getEmailVerified());
        assertEquals(1, saved.getPhoneVerified());
    }

    @Test
    void importUsersFromExcel_phoneAlreadyExists_shouldReturnError() throws IOException {
        UserImportExcelRowData row = new UserImportExcelRowData();
        row.setPhone("13800138000");
        row.setNickname("重复手机");
        row.setPassword("");
        row.setUserType("1");

        MultipartFile file = createExcelFile(List.of(row));

        when(platformUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        AdminUserImportResultVO result = adminUserService.importUsersFromExcel(file);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getImportedCount());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getErrors().get(0).contains("手机号"));
        verify(platformUserMapper, never()).insert(any(PlatformUser.class));
    }

    @Test
    void importUsersFromExcel_noEmailNorPhone_shouldReturnError() throws IOException {
        UserImportExcelRowData row = new UserImportExcelRowData();
        row.setEmail("");
        row.setPhone("");
        row.setNickname("无账号");
        row.setPassword("");
        row.setUserType("1");

        MultipartFile file = createExcelFile(List.of(row));

        AdminUserImportResultVO result = adminUserService.importUsersFromExcel(file);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getImportedCount());
        assertTrue(result.getErrors().get(0).getErrors().get(0).contains("邮箱/手机号"));
        verify(platformUserMapper, never()).insert(any(PlatformUser.class));
    }

    private MultipartFile createExcelFile(List<UserImportExcelRowData> rows) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        EasyExcel.write(outputStream, UserImportExcelRowData.class)
                .sheet("用户导入模板")
                .doWrite(rows);
        return new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                outputStream.toByteArray());
    }
}
