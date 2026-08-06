package com.aichuangzuo.user.modules.membership.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.user.modules.earnings.mapper.EarningsRecordMapper;
import com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribePreviewRequest;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.dto.request.UpgradePreviewRequest;
import com.aichuangzuo.user.modules.membership.entity.Order;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.OrderMapper;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.membership.vo.MembershipStatusVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;
import com.aichuangzuo.user.modules.membership.vo.UpgradePreviewVO;
import com.aichuangzuo.user.modules.message.entity.Message;
import com.aichuangzuo.user.modules.message.mapper.MessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class MembershipServiceTest {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMembershipMapper userMembershipMapper;

    @Autowired
    private UserInviteRelationMapper userInviteRelationMapper;

    @Autowired
    private UserCoinRecordMapper userCoinRecordMapper;

    @Autowired
    private EarningsRecordMapper earningsRecordMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void subscribe_invalidPayCode_throwsBusinessException() {
        User user = createUser("sub-pay-code@test.com");
        SubscribeRequest request = buildRequest("pro", "year", "000000", new BigDecimal("503.2"));

        try {
            membershipService.subscribe(user.getId(), request);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("支付码错误"));
            return;
        }
        throw new AssertionError("应抛出支付码错误异常");
    }

    @Test
    void subscribe_newMembership_createsMembershipAndOrder() {
        User user = createUser("sub-new@test.com");
        SubscribeRequest request = buildRequest("pro", "year", "123456", new BigDecimal("503.2"));

        SubscribeResultVO result = membershipService.subscribe(user.getId(), request);

        assertNotNull(result.getOrderNo());
        assertTrue(result.getOrderNo().startsWith("SUB"));
        assertEquals("pro", result.getLevel());
        assertEquals(365, result.getDays());
        assertNotNull(result.getExpiresAt());
        assertFalse(result.isInviterRewarded());
        assertEquals(0, result.getRewardAmount().compareTo(BigDecimal.ZERO));

        UserMembership membership = userMembershipMapper.selectByUserId(user.getId());
        assertNotNull(membership);
        assertEquals("pro", membership.getLevel());
        assertEquals(LocalDate.now().plusDays(365), membership.getExpiresAt());
    }

    @Test
    void subscribe_extendMembership_extendsFromExistingExpiry() {
        User user = createUser("sub-extend@test.com");
        UserMembership existing = new UserMembership();
        existing.setUserId(user.getId());
        existing.setLevel("basic");
        existing.setStartedAt(LocalDate.now().minusDays(30));
        existing.setExpiresAt(LocalDate.now().plusDays(10));
        existing.setTenantId(0L);
        userMembershipMapper.insert(existing);

        SubscribeRequest request = buildRequest("basic", "quarter", "123456", new BigDecimal("53.1"));
        SubscribeResultVO result = membershipService.subscribe(user.getId(), request);

        UserMembership membership = userMembershipMapper.selectByUserId(user.getId());
        assertEquals("basic", membership.getLevel());
        assertEquals(LocalDate.now().plusDays(10).plusDays(90), membership.getExpiresAt());
        assertEquals(90, result.getDays());
    }

    @Test
    void subscribe_upgradeAppliesCreditAndStartsFresh() {
        User user = createUser("sub-upgrade@test.com");
        UserMembership existing = new UserMembership();
        existing.setUserId(user.getId());
        existing.setLevel("basic");
        existing.setStartedAt(LocalDate.now().minusDays(30));
        existing.setExpiresAt(LocalDate.now().plusDays(10));
        existing.setTenantId(0L);
        userMembershipMapper.insert(existing);

        Order firstOrder = new Order();
        firstOrder.setOrderNo("SUB" + System.nanoTime());
        firstOrder.setUserId(user.getId());
        firstOrder.setPlanKey("basic");
        firstOrder.setCycle("month");
        firstOrder.setAmount(new BigDecimal("29.90"));
        firstOrder.setStatus(1);
        firstOrder.setPaidAt(LocalDateTime.now());
        firstOrder.setTenantId(0L);
        orderMapper.insert(firstOrder);

        // basic -> pro 为升级，抵扣剩余 10 天价值（约 9.97），实付约 151.73
        SubscribeRequest request = buildRequest("pro", "quarter", "123456", new BigDecimal("151.73"));
        SubscribeResultVO result = membershipService.subscribe(user.getId(), request);

        UserMembership membership = userMembershipMapper.selectByUserId(user.getId());
        assertEquals("pro", membership.getLevel());
        assertEquals(LocalDate.now().plusDays(90), membership.getExpiresAt());
        assertEquals(90, result.getDays());
    }

    @Test
    void previewUpgrade_withActiveMembership_returnsCreditBreakdown() {
        User user = createUser("preview-upgrade@test.com");
        UserMembership existing = new UserMembership();
        existing.setUserId(user.getId());
        existing.setLevel("basic");
        existing.setStartedAt(LocalDate.now().minusDays(30));
        existing.setExpiresAt(LocalDate.now().plusDays(10));
        existing.setTenantId(0L);
        userMembershipMapper.insert(existing);

        Order firstOrder = new Order();
        firstOrder.setOrderNo("SUB" + System.nanoTime());
        firstOrder.setUserId(user.getId());
        firstOrder.setPlanKey("basic");
        firstOrder.setCycle("month");
        firstOrder.setAmount(new BigDecimal("29.90"));
        firstOrder.setStatus(1);
        firstOrder.setPaidAt(LocalDateTime.now());
        firstOrder.setTenantId(0L);
        orderMapper.insert(firstOrder);

        UpgradePreviewRequest request = new UpgradePreviewRequest();
        request.setPlanKey("pro");
        request.setCycle("month");

        UpgradePreviewVO vo = membershipService.previewUpgrade(user.getId(), request);

        assertTrue(vo.isHasMembership());
        assertEquals("basic", vo.getCurrentPlanKey());
        assertEquals("pro", vo.getTargetPlanKey());
        assertTrue(vo.isUpgrade());
        assertEquals(10, vo.getRemainingDays());
        assertEquals(0, new BigDecimal("59.90").compareTo(vo.getOriginalPrice()));
        assertEquals(0, new BigDecimal("9.97").compareTo(vo.getCreditAmount()));
        assertEquals(0, new BigDecimal("49.93").compareTo(vo.getFinalPrice()));
        assertEquals(LocalDate.now().plusDays(30), LocalDate.parse(vo.getNewExpiresAt()));
    }

    @Test
    void previewUpgrade_samePlanLongerCycle_returnsCreditBreakdown() {
        User user = createUser("preview-upgrade-same-cycle@test.com");
        UserMembership existing = new UserMembership();
        existing.setUserId(user.getId());
        existing.setLevel("basic");
        existing.setStartedAt(LocalDate.now().minusDays(30));
        existing.setExpiresAt(LocalDate.now().plusDays(10));
        existing.setTenantId(0L);
        userMembershipMapper.insert(existing);

        Order firstOrder = new Order();
        firstOrder.setOrderNo("SUB" + System.nanoTime());
        firstOrder.setUserId(user.getId());
        firstOrder.setPlanKey("basic");
        firstOrder.setCycle("month");
        firstOrder.setAmount(new BigDecimal("29.90"));
        firstOrder.setStatus(1);
        firstOrder.setPaidAt(LocalDateTime.now());
        firstOrder.setTenantId(0L);
        orderMapper.insert(firstOrder);

        UpgradePreviewRequest request = new UpgradePreviewRequest();
        request.setPlanKey("basic");
        request.setCycle("quarter");

        UpgradePreviewVO vo = membershipService.previewUpgrade(user.getId(), request);

        assertTrue(vo.isHasMembership());
        assertEquals("basic", vo.getCurrentPlanKey());
        assertEquals("basic", vo.getTargetPlanKey());
        assertTrue(vo.isUpgrade());
        assertEquals(10, vo.getRemainingDays());
        assertEquals(0, new BigDecimal("53.10").compareTo(vo.getOriginalPrice()));
        assertEquals(0, new BigDecimal("9.97").compareTo(vo.getCreditAmount()));
        assertEquals(0, new BigDecimal("43.13").compareTo(vo.getFinalPrice()));
        assertEquals(LocalDate.now().plusDays(90), LocalDate.parse(vo.getNewExpiresAt()));
    }

    @Test
    void previewUpgrade_shorterCycle_throwsBusinessException() {
        User user = createUser("preview-upgrade-cycle@test.com");
        UserMembership existing = new UserMembership();
        existing.setUserId(user.getId());
        existing.setLevel("basic");
        existing.setStartedAt(LocalDate.now().minusDays(30));
        existing.setExpiresAt(LocalDate.now().plusDays(10));
        existing.setTenantId(0L);
        userMembershipMapper.insert(existing);

        Order firstOrder = new Order();
        firstOrder.setOrderNo("SUB" + System.nanoTime());
        firstOrder.setUserId(user.getId());
        firstOrder.setPlanKey("basic");
        firstOrder.setCycle("quarter");
        firstOrder.setAmount(new BigDecimal("53.10"));
        firstOrder.setStatus(1);
        firstOrder.setPaidAt(LocalDateTime.now());
        firstOrder.setTenantId(0L);
        orderMapper.insert(firstOrder);

        UpgradePreviewRequest request = new UpgradePreviewRequest();
        request.setPlanKey("pro");
        request.setCycle("month");

        try {
            membershipService.previewUpgrade(user.getId(), request);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("升级不能缩短当前订阅周期"));
            return;
        }
        throw new AssertionError("应抛出周期缩短异常");
    }

    @Test
    void subscribe_withInviter_rewardsInviterFirstPurchase() {
        User inviter = createUser("sub-inviter@test.com");
        User invitee = createUser("sub-invitee@test.com");
        createInviteRelation(inviter, invitee);

        SubscribeRequest request = buildRequest("pro", "year", "123456", new BigDecimal("503.2"));
        SubscribeResultVO result = membershipService.subscribe(invitee.getId(), request);

        assertTrue(result.isInviterRewarded());
        assertEquals(0, result.getRewardAmount().compareTo(new BigDecimal("503.20")));

        Long rewardCount = userCoinRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord>()
                        .eq(com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord::getUserId, inviter.getId())
                        .eq(com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord::getBizType, "invite_reward")
        );
        assertEquals(1L, rewardCount);

        EarningsRecord earnings = earningsRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EarningsRecord>()
                        .eq(EarningsRecord::getUserId, inviter.getId())
                        .eq(EarningsRecord::getType, "INVITE_REWARD")
                        .eq(EarningsRecord::getSourceType, "invite")
                        .eq(EarningsRecord::getSourceId, invitee.getId().toString())
                        .eq(EarningsRecord::getStatus, 1)
        );
        assertNotNull(earnings);
        assertEquals(0, earnings.getAmount().compareTo(new BigDecimal("503.20")));
        assertEquals(0, earnings.getCommissionRate().compareTo(new BigDecimal("0.10")));
        assertEquals(Integer.valueOf(1), earnings.getIsFirstPurchase());
        assertEquals("pro", earnings.getPlanKey());
        assertEquals("year", earnings.getCycle());

        Message message = messageMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Message>()
                        .eq(Message::getTargetUserId, inviter.getId())
                        .eq(Message::getMsgType, "reward"));
        assertNotNull(message);
        assertEquals("邀请奖励", message.getTitle());
        assertTrue(message.getSummary().contains("503.20"));
    }

    @Test
    void subscribe_invalidAmount_throwsBusinessException() {
        User user = createUser("sub-amount@test.com");
        SubscribeRequest request = buildRequest("pro", "year", "123456", new BigDecimal("1.00"));

        try {
            membershipService.subscribe(user.getId(), request);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("支付金额"));
            return;
        }
        throw new AssertionError("应抛出支付金额错误异常");
    }

    @Test
    void subscribe_newcomerFlagshipYear_usesDiscountedAmount() {
        User user = createUser("sub-newcomer@test.com");
        SubscribeRequest request = buildRequest("flagship", "year", "123456", new BigDecimal("671.36"));

        SubscribeResultVO result = membershipService.subscribe(user.getId(), request);

        assertNotNull(result.getOrderNo());
        assertEquals("flagship", result.getLevel());
        assertEquals(365, result.getDays());
        assertFalse(result.isInviterRewarded());

        UserMembership membership = userMembershipMapper.selectByUserId(user.getId());
        assertNotNull(membership);
        assertEquals("flagship", membership.getLevel());
        assertEquals(LocalDate.now().plusDays(365), membership.getExpiresAt());
    }

    @Test
    void getMyMembership_expired_returnsFalse() {
        User user = createUser("sub-expired@test.com");
        UserMembership membership = new UserMembership();
        membership.setUserId(user.getId());
        membership.setLevel("pro");
        membership.setStartedAt(LocalDate.now().minusDays(400));
        membership.setExpiresAt(LocalDate.now().minusDays(10));
        membership.setTenantId(0L);
        userMembershipMapper.insert(membership);

        MembershipStatusVO vo = membershipService.getMyMembership(user.getId());

        assertFalse(vo.isHasMembership());
    }

    @Test
    void extendMembership_withoutExistingMembership_startsFromToday() {
        User user = createUser("extend-new@test.com");

        membershipService.extendMembership(user.getId(), "pro", 7);

        UserMembership membership = userMembershipMapper.selectByUserId(user.getId());
        assertNotNull(membership);
        assertEquals("pro", membership.getLevel());
        assertEquals(LocalDate.now().plusDays(7), membership.getExpiresAt());
    }

    @Test
    void extendMembership_withExistingMembership_extendsFromExpiry() {
        User user = createUser("extend-existing@test.com");
        UserMembership existing = new UserMembership();
        existing.setUserId(user.getId());
        existing.setLevel("basic");
        existing.setStartedAt(LocalDate.now().minusDays(30));
        existing.setExpiresAt(LocalDate.now().plusDays(10));
        existing.setTenantId(0L);
        userMembershipMapper.insert(existing);

        membershipService.extendMembership(user.getId(), "pro", 5);

        UserMembership membership = userMembershipMapper.selectByUserId(user.getId());
        assertEquals("pro", membership.getLevel());
        assertEquals(LocalDate.now().plusDays(10).plusDays(5), membership.getExpiresAt());
    }

    @Test
    void subscribe_withInviter_renewalUsesFivePercentCommission() {
        User inviter = createUser("sub-inviter-renewal@test.com");
        User invitee = createUser("sub-invitee-renewal@test.com");
        createInviteRelation(inviter, invitee);

        Order firstOrder = new Order();
        firstOrder.setOrderNo("SUB" + System.nanoTime());
        firstOrder.setUserId(invitee.getId());
        firstOrder.setPlanKey("basic");
        firstOrder.setCycle("month");
        firstOrder.setAmount(new BigDecimal("29.90"));
        firstOrder.setStatus(1);
        firstOrder.setPaidAt(LocalDateTime.now());
        firstOrder.setTenantId(0L);
        orderMapper.insert(firstOrder);

        SubscribeRequest request = buildRequest("pro", "year", "123456", new BigDecimal("503.2"));
        SubscribeResultVO result = membershipService.subscribe(invitee.getId(), request);

        assertTrue(result.isInviterRewarded());
        assertEquals(0, result.getRewardAmount().compareTo(new BigDecimal("251.60")));

        EarningsRecord earnings = earningsRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EarningsRecord>()
                        .eq(EarningsRecord::getUserId, inviter.getId())
                        .eq(EarningsRecord::getType, "INVITE_REWARD")
                        .eq(EarningsRecord::getSourceId, invitee.getId().toString())
        );
        assertNotNull(earnings);
        assertEquals(0, earnings.getAmount().compareTo(new BigDecimal("251.60")));
        assertEquals(0, earnings.getCommissionRate().compareTo(new BigDecimal("0.05")));
        assertEquals(Integer.valueOf(0), earnings.getIsFirstPurchase());
    }

    @Test
    void previewSubscribe_returnsCoinInfo() {
        User user = createUser("preview-sub@test.com");
        setCoinBalance(user.getId(), new BigDecimal("200"));

        SubscribePreviewRequest request = new SubscribePreviewRequest();
        request.setPlanKey("pro");
        request.setCycle("month");

        com.aichuangzuo.user.modules.membership.vo.SubscribePreviewVO vo = membershipService.previewSubscribe(user.getId(), request);

        assertEquals("pro", vo.getPlanKey());
        assertEquals("month", vo.getCycle());
        assertEquals(0, new BigDecimal("59.90").compareTo(vo.getOriginalPrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getCreditAmount()));
        assertEquals(0, new BigDecimal("59.90").compareTo(vo.getFinalPrice()));
        assertEquals(0, new BigDecimal("200").compareTo(vo.getCoinBalance()));
        assertEquals(Long.valueOf(200L), vo.getMaxCoinAmount());
        assertEquals(Integer.valueOf(10), vo.getCoinToYuanRatio());
    }

    @Test
    void previewSubscribe_maxCoinAmountCappedByCash() {
        User user = createUser("preview-sub-capped@test.com");
        setCoinBalance(user.getId(), new BigDecimal("1000"));

        SubscribePreviewRequest request = new SubscribePreviewRequest();
        request.setPlanKey("pro");
        request.setCycle("month");

        com.aichuangzuo.user.modules.membership.vo.SubscribePreviewVO vo = membershipService.previewSubscribe(user.getId(), request);

        assertEquals(Long.valueOf(599L), vo.getMaxCoinAmount());
    }

    @Test
    void previewUpgrade_returnsCoinInfo() {
        User user = createUser("preview-upgrade-coin@test.com");
        UserMembership existing = new UserMembership();
        existing.setUserId(user.getId());
        existing.setLevel("basic");
        existing.setStartedAt(LocalDate.now().minusDays(30));
        existing.setExpiresAt(LocalDate.now().plusDays(10));
        existing.setTenantId(0L);
        userMembershipMapper.insert(existing);

        Order firstOrder = new Order();
        firstOrder.setOrderNo("SUB" + System.nanoTime());
        firstOrder.setUserId(user.getId());
        firstOrder.setPlanKey("basic");
        firstOrder.setCycle("month");
        firstOrder.setAmount(new BigDecimal("29.90"));
        firstOrder.setStatus(1);
        firstOrder.setPaidAt(LocalDateTime.now());
        firstOrder.setTenantId(0L);
        orderMapper.insert(firstOrder);

        setCoinBalance(user.getId(), new BigDecimal("500"));

        UpgradePreviewRequest request = new UpgradePreviewRequest();
        request.setPlanKey("pro");
        request.setCycle("month");

        UpgradePreviewVO vo = membershipService.previewUpgrade(user.getId(), request);

        assertEquals(0, new BigDecimal("500").compareTo(vo.getCoinBalance()));
        assertEquals(Long.valueOf(499L), vo.getMaxCoinAmount());
        assertEquals(Integer.valueOf(10), vo.getCoinToYuanRatio());
    }

    @Test
    void subscribe_withCoinDiscount_reducesCashAndBalance() {
        User user = createUser("sub-coin@test.com");
        setCoinBalance(user.getId(), new BigDecimal("200"));

        SubscribeRequest request = buildRequest("pro", "month", "123456", new BigDecimal("39.90"));
        request.setCoinAmount(new BigDecimal("200"));

        SubscribeResultVO result = membershipService.subscribe(user.getId(), request);

        assertEquals(Long.valueOf(200L), result.getCoinAmount());
        assertEquals(0, new BigDecimal("20.00").compareTo(result.getCoinDiscountYuan()));
        assertEquals(0, new BigDecimal("39.90").compareTo(result.getCashAmount()));

        Order order = orderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, result.getOrderNo()));
        assertNotNull(order);
        assertEquals(Long.valueOf(200L), order.getCoinAmount());
        assertEquals(0, new BigDecimal("20.00").compareTo(order.getCoinDiscount()));
        assertEquals(0, new BigDecimal("59.90").compareTo(order.getTotalAmount()));

        User updatedUser = userMapper.selectById(user.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedUser.getCoinBalance()));

        UserCoinRecord coinRecord = userCoinRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserCoinRecord>()
                        .eq(UserCoinRecord::getUserId, user.getId())
                        .eq(UserCoinRecord::getBizType, "subscribe_coin_discount"));
        assertNotNull(coinRecord);
        assertEquals(0, new BigDecimal("200").compareTo(coinRecord.getAmount()));

        EarningsRecord earningsRecord = earningsRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EarningsRecord>()
                        .eq(EarningsRecord::getUserId, user.getId())
                        .eq(EarningsRecord::getType, "COIN_DEDUCTION")
                        .eq(EarningsRecord::getSourceType, "subscribe_coin_discount")
                        .eq(EarningsRecord::getSourceId, order.getId().toString()));
        assertNotNull(earningsRecord);
        assertEquals(0, new BigDecimal("-200").compareTo(earningsRecord.getAmount()));
        assertEquals("订阅抵扣", earningsRecord.getTitle());
        assertTrue(earningsRecord.getDescription().contains("200"));
    }

    @Test
    void subscribe_fullCoinDiscount_zeroCash() {
        User user = createUser("sub-full-coin@test.com");
        setCoinBalance(user.getId(), new BigDecimal("599"));

        SubscribeRequest request = buildRequest("pro", "month", "123456", BigDecimal.ZERO);
        request.setCoinAmount(new BigDecimal("599"));

        SubscribeResultVO result = membershipService.subscribe(user.getId(), request);

        assertEquals(Long.valueOf(599L), result.getCoinAmount());
        assertEquals(0, new BigDecimal("59.90").compareTo(result.getCoinDiscountYuan()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getCashAmount()));

        User updatedUser = userMapper.selectById(user.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedUser.getCoinBalance()));
    }

    @Test
    void subscribe_coinAmountExceedsBalance_throwsException() {
        User user = createUser("sub-coin-exceed@test.com");
        setCoinBalance(user.getId(), new BigDecimal("100"));

        SubscribeRequest request = buildRequest("pro", "month", "123456", new BigDecimal("49.90"));
        request.setCoinAmount(new BigDecimal("200"));

        try {
            membershipService.subscribe(user.getId(), request);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("创作币抵扣金额不正确"));
            return;
        }
        throw new AssertionError("应抛出创作币抵扣金额错误异常");
    }

    @Test
    void subscribe_fractionalCoinAmount_throwsException() {
        User user = createUser("sub-coin-fractional@test.com");
        setCoinBalance(user.getId(), new BigDecimal("100"));

        SubscribeRequest request = buildRequest("pro", "month", "123456", new BigDecimal("54.90"));
        request.setCoinAmount(new BigDecimal("50.5"));

        try {
            membershipService.subscribe(user.getId(), request);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("创作币抵扣金额不正确"));
            return;
        }
        throw new AssertionError("应抛出创作币抵扣金额错误异常");
    }

    private User createUser(String email) {
        User user = new User();
        user.setBizNo("B" + System.nanoTime());
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setInviteCode("X" + System.nanoTime());
        user.setUserStatus(1);
        user.setEmailVerified(1);
        user.setCoinBalance(BigDecimal.ZERO);
        userMapper.insert(user);
        return user;
    }

    private void createInviteRelation(User inviter, User invitee) {
        UserInviteRelation relation = new UserInviteRelation();
        relation.setInviterId(inviter.getId());
        relation.setInviteeId(invitee.getId());
        relation.setInviteCode(inviter.getInviteCode());
        relation.setSourceType(2);
        relation.setEffectiveStatus(1);
        userInviteRelationMapper.insert(relation);
    }

    private void setCoinBalance(Long userId, BigDecimal balance) {
        User user = userMapper.selectById(userId);
        user.setCoinBalance(balance);
        userMapper.updateById(user);
    }

    private SubscribeRequest buildRequest(String planKey, String cycle, String payCode, BigDecimal amount) {
        SubscribeRequest request = new SubscribeRequest();
        request.setPlanKey(planKey);
        request.setCycle(cycle);
        request.setPayCode(payCode);
        request.setAmount(amount);
        return request;
    }
}
