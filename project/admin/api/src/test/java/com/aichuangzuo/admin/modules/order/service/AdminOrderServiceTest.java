package com.aichuangzuo.admin.modules.order.service;

import com.aichuangzuo.admin.modules.order.dto.request.MembershipAdjustRequest;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipGrantRequest;
import com.aichuangzuo.admin.modules.order.entity.AdminMembership;
import com.aichuangzuo.admin.modules.order.entity.AdminOrderView;
import com.aichuangzuo.admin.modules.order.enums.AdminOrderErrorCode;
import com.aichuangzuo.admin.modules.order.mapper.AdminMembershipMapper;
import com.aichuangzuo.admin.modules.order.mapper.AdminOrderMapper;
import com.aichuangzuo.admin.modules.order.service.impl.AdminOrderServiceImpl;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private AdminOrderMapper orderMapper;

    @Mock
    private AdminMembershipMapper membershipMapper;

    @InjectMocks
    private AdminOrderServiceImpl orderService;

    private AdminOrderView paidOrder;
    private AdminOrderView pendingOrder;

    @BeforeEach
    void setUp() {
        paidOrder = new AdminOrderView();
        paidOrder.setId(1L);
        paidOrder.setOrderNo("SUB260715000001");
        paidOrder.setUserId(5L);
        paidOrder.setPlanKey("pro");
        paidOrder.setCycle("month");
        paidOrder.setAmount(new BigDecimal("59.90"));
        paidOrder.setStatus(1);
        paidOrder.setPaidAt(LocalDateTime.now());

        pendingOrder = new AdminOrderView();
        pendingOrder.setId(2L);
        pendingOrder.setOrderNo("SUB260715000002");
        pendingOrder.setUserId(5L);
        pendingOrder.setPlanKey("basic");
        pendingOrder.setCycle("quarter");
        pendingOrder.setAmount(new BigDecimal("80.70"));
        pendingOrder.setStatus(0);
    }

    // ── markPaid ──

    @Test
    void markPaid_orderNotFound_throws() {
        when(orderMapper.selectDetailById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.markPaid(99L, 1L));
        assertEquals(AdminOrderErrorCode.ORDER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void markPaid_alreadyPaid_throws() {
        when(orderMapper.selectDetailById(1L)).thenReturn(paidOrder);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.markPaid(1L, 1L));
        assertEquals(AdminOrderErrorCode.ORDER_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void markPaid_pendingOrder_success() {
        when(orderMapper.selectDetailById(2L)).thenReturn(pendingOrder);
        when(orderMapper.markPaid(eq(2L), eq(1L), any())).thenReturn(1);
        when(membershipMapper.selectByUserId(5L)).thenReturn(null);
        when(membershipMapper.insertMembership(any())).thenReturn(1);
        when(membershipMapper.updateUserMembershipFields(eq(5L), any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> orderService.markPaid(2L, 1L));
        verify(orderMapper).markPaid(eq(2L), eq(1L), any());
        verify(membershipMapper).insertMembership(argThat(m ->
                "basic".equals(m.getLevel()) && m.getUserId() == 5L));
    }

    // ── refund ──

    @Test
    void refund_orderNotPaid_throws() {
        when(orderMapper.selectDetailById(2L)).thenReturn(pendingOrder);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.refund(2L, "不想要了", 1L));
        assertEquals(AdminOrderErrorCode.ORDER_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void refund_blankReason_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.refund(1L, "  ", 1L));
        assertEquals(AdminOrderErrorCode.REFUND_REASON_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void refund_paidOrder_success() {
        AdminMembership membership = new AdminMembership();
        membership.setId(1L);
        membership.setUserId(5L);
        membership.setLevel("pro");
        membership.setStartedAt(LocalDate.now().minusDays(5));
        membership.setExpiresAt(LocalDate.now().plusDays(25));

        when(orderMapper.selectDetailById(1L)).thenReturn(paidOrder);
        when(orderMapper.refund(eq(1L), eq("不想要了"), eq(1L), any())).thenReturn(1);
        when(membershipMapper.selectByUserId(5L)).thenReturn(membership);
        when(membershipMapper.updateMembership(any())).thenReturn(1);
        when(membershipMapper.updateUserMembershipFields(eq(5L), any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> orderService.refund(1L, "不想要了", 1L));
        // 退款直接取消会员 → expiresAt 设为昨天
        verify(membershipMapper).updateMembership(argThat(m ->
                m.getExpiresAt().equals(LocalDate.now().minusDays(1))));
    }

    // ── cancel ──

    @Test
    void cancel_pendingOrder_success() {
        when(orderMapper.selectDetailById(2L)).thenReturn(pendingOrder);
        when(orderMapper.cancel(2L, 1L)).thenReturn(1);

        assertDoesNotThrow(() -> orderService.cancel(2L, 1L));
        verify(orderMapper).cancel(2L, 1L);
    }

    @Test
    void cancel_paidOrder_throws() {
        when(orderMapper.selectDetailById(1L)).thenReturn(paidOrder);
        assertThrows(BusinessException.class, () -> orderService.cancel(1L, 1L));
    }

    // ── adjustMembership ──

    @Test
    void adjustMembership_userNotFound_throws() {
        MembershipAdjustRequest req = new MembershipAdjustRequest();
        req.setUserId(99L);
        req.setLevel("pro");
        req.setExpiresAt(LocalDate.now().plusDays(30));

        when(membershipMapper.userExists(99L)).thenReturn(0);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.adjustMembership(req, 1L));
        assertEquals(AdminOrderErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void adjustMembership_existingMembership_updates() {
        MembershipAdjustRequest req = new MembershipAdjustRequest();
        req.setUserId(5L);
        req.setLevel("flagship");
        req.setExpiresAt(LocalDate.now().plusDays(365));

        AdminMembership existing = new AdminMembership();
        existing.setId(1L);
        existing.setUserId(5L);
        existing.setLevel("basic");

        when(membershipMapper.userExists(5L)).thenReturn(1);
        when(membershipMapper.selectByUserId(5L)).thenReturn(existing);
        when(membershipMapper.updateMembership(any())).thenReturn(1);
        when(membershipMapper.updateUserMembershipFields(eq(5L), any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> orderService.adjustMembership(req, 1L));
        verify(membershipMapper).updateMembership(argThat(m ->
                "flagship".equals(m.getLevel())));
    }

    @Test
    void adjustMembership_noExistingMembership_inserts() {
        MembershipAdjustRequest req = new MembershipAdjustRequest();
        req.setUserId(5L);
        req.setLevel("pro");
        req.setExpiresAt(LocalDate.now().plusDays(90));

        when(membershipMapper.userExists(5L)).thenReturn(1);
        when(membershipMapper.selectByUserId(5L)).thenReturn(null);
        when(membershipMapper.insertMembership(any())).thenReturn(1);
        when(membershipMapper.updateUserMembershipFields(eq(5L), any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> orderService.adjustMembership(req, 1L));
        verify(membershipMapper).insertMembership(argThat(m ->
                "pro".equals(m.getLevel()) && m.getUserId() == 5L));
    }

    // ── grantMembership ──

    @Test
    void grantMembership_createsZeroOrderAndActivates() {
        MembershipGrantRequest req = new MembershipGrantRequest();
        req.setUserId(5L);
        req.setPlanKey("pro");
        req.setCycle("month");
        req.setRemark("活动赠送");

        when(membershipMapper.userExists(5L)).thenReturn(1);
        when(membershipMapper.selectByUserId(5L)).thenReturn(null);
        when(membershipMapper.insertMembership(any())).thenReturn(1);
        when(membershipMapper.updateUserMembershipFields(eq(5L), any(), any())).thenReturn(1);
        when(orderMapper.insertGrantOrder(anyString(), eq(5L), eq("pro"), eq("month"),
                anyString(), eq(1L), any())).thenReturn(1);

        assertDoesNotThrow(() -> orderService.grantMembership(req, 1L));
        verify(orderMapper).insertGrantOrder(anyString(), eq(5L), eq("pro"), eq("month"),
                argThat(r -> r.contains("活动赠送")), eq(1L), any());
        verify(membershipMapper).insertMembership(argThat(m ->
                "pro".equals(m.getLevel())));
    }

    @Test
    void grantMembership_userNotFound_throws() {
        MembershipGrantRequest req = new MembershipGrantRequest();
        req.setUserId(99L);
        req.setPlanKey("pro");
        req.setCycle("month");

        when(membershipMapper.userExists(99L)).thenReturn(0);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.grantMembership(req, 1L));
        assertEquals(AdminOrderErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }
}
