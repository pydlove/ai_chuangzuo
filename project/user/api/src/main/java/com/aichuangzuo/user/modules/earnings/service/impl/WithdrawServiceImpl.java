package com.aichuangzuo.user.modules.earnings.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.earnings.dto.request.RealNameRequest;
import com.aichuangzuo.user.modules.earnings.dto.request.WithdrawApplyRequest;
import com.aichuangzuo.user.modules.earnings.dto.request.WithdrawProcessRequest;
import com.aichuangzuo.user.modules.earnings.entity.WithdrawRequest;
import com.aichuangzuo.shared.enums.error.WithdrawErrorCode;
import com.aichuangzuo.user.modules.earnings.mapper.WithdrawRequestMapper;
import com.aichuangzuo.user.modules.earnings.service.WithdrawService;
import com.aichuangzuo.user.modules.earnings.vo.RealNameVO;
import com.aichuangzuo.user.modules.earnings.vo.WithdrawRequestVO;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户实名认证与提现服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawServiceImpl implements WithdrawService {

    private static final String BIZ_NO_PREFIX = "WD";
    private static final BigDecimal MIN_WITHDRAW_AMOUNT = new BigDecimal("1000");
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;

    private final UserMapper userMapper;
    private final WithdrawRequestMapper withdrawRequestMapper;
    private final CoinRecordService coinRecordService;

    @Override
    public RealNameVO getRealName(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getRealNameVerified() == 1)) {
            return null;
        }
        RealNameVO vo = new RealNameVO();
        vo.setRealName(user.getRealName());
        vo.setIdCard(maskIdCard(user.getIdCard()));
        vo.setVerified(true);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitRealName(Long userId, RealNameRequest request) {
        if (!StringUtils.hasText(request.getRealName())
                || !StringUtils.hasText(request.getIdCard())
                || !isValidIdCard(request.getIdCard())) {
            throw new BusinessException(WithdrawErrorCode.REAL_NAME_INVALID);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(WithdrawErrorCode.REAL_NAME_INVALID);
        }
        user.setRealName(request.getRealName().trim());
        user.setIdCard(request.getIdCard().trim().toUpperCase());
        user.setRealNameVerified(1);
        userMapper.updateById(user);
        log.info("用户完成实名认证 userId={}", userId);
    }

    @Override
    public List<WithdrawRequestVO> listWithdrawRequests(Long userId) {
        LambdaQueryWrapper<WithdrawRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WithdrawRequest::getUserId, userId)
                .eq(WithdrawRequest::getIsDeleted, 0)
                .orderByDesc(WithdrawRequest::getCreatedAt);
        return withdrawRequestMapper.selectList(wrapper).stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String applyWithdraw(Long userId, WithdrawApplyRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getRealNameVerified() == 1)) {
            throw new BusinessException(WithdrawErrorCode.REAL_NAME_NOT_VERIFIED);
        }
        if (!StringUtils.hasText(request.getAccount())) {
            throw new BusinessException(WithdrawErrorCode.WITHDRAW_ACCOUNT_INVALID);
        }
        if (request.getAmount() == null || request.getAmount().compareTo(MIN_WITHDRAW_AMOUNT) < 0) {
            throw new BusinessException(WithdrawErrorCode.WITHDRAW_AMOUNT_INVALID);
        }

        LambdaQueryWrapper<WithdrawRequest> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(WithdrawRequest::getUserId, userId)
                .eq(WithdrawRequest::getStatus, STATUS_PENDING)
                .eq(WithdrawRequest::getIsDeleted, 0);
        if (withdrawRequestMapper.selectCount(pendingWrapper) > 0) {
            throw new BusinessException(WithdrawErrorCode.WITHDRAW_PENDING_EXISTS);
        }

        String bizNo = generateBizNo();
        coinRecordService.spend(userId, "withdraw", request.getAmount(), bizNo, "提现申请扣减");

        WithdrawRequest record = new WithdrawRequest();
        record.setBizNo(bizNo);
        record.setUserId(userId);
        record.setAmount(request.getAmount());
        record.setAccount(request.getAccount().trim());
        record.setName(user.getRealName());
        record.setStatus(STATUS_PENDING);
        record.setTenantId(0L);
        withdrawRequestMapper.insert(record);

        log.info("用户提交提现申请 userId={}, bizNo={}, amount={}", userId, bizNo, request.getAmount());
        return bizNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processWithdraw(String bizNo, Long adminUserId, WithdrawProcessRequest request) {
        if (request.getStatus() == null || (request.getStatus() != STATUS_APPROVED && request.getStatus() != STATUS_REJECTED)) {
            throw new BusinessException(WithdrawErrorCode.WITHDRAW_AMOUNT_INVALID);
        }

        WithdrawRequest record = withdrawRequestMapper.selectOne(
                new LambdaQueryWrapper<WithdrawRequest>()
                        .eq(WithdrawRequest::getBizNo, bizNo)
                        .eq(WithdrawRequest::getIsDeleted, 0));
        if (record == null) {
            throw new BusinessException(WithdrawErrorCode.WITHDRAW_NOT_FOUND);
        }
        if (record.getStatus() == null || record.getStatus() != STATUS_PENDING) {
            throw new BusinessException(WithdrawErrorCode.WITHDRAW_ALREADY_PROCESSED);
        }

        if (request.getStatus() == STATUS_REJECTED) {
            if (!StringUtils.hasText(request.getRemark())) {
                throw new BusinessException(WithdrawErrorCode.WITHDRAW_REJECT_REASON_EMPTY);
            }
            coinRecordService.grant(record.getUserId(), "withdraw_refund", record.getAmount(),
                    record.getBizNo(), "提现被拒绝退回");
        }

        record.setStatus(request.getStatus());
        record.setProcessedAt(LocalDateTime.now());
        record.setProcessedBy(adminUserId);
        record.setResultRemark(request.getRemark());
        withdrawRequestMapper.updateById(record);

        log.info("管理员处理提现申请 bizNo={}, status={}, adminUserId={}", bizNo, request.getStatus(), adminUserId);
    }

    private WithdrawRequestVO toVo(WithdrawRequest record) {
        WithdrawRequestVO vo = new WithdrawRequestVO();
        vo.setBizNo(record.getBizNo());
        vo.setAmount(record.getAmount());
        vo.setAccount(record.getAccount());
        vo.setName(record.getName());
        User user = userMapper.selectById(record.getUserId());
        vo.setNickname(maskNickname(user == null ? null : user.getNickname()));
        vo.setStatus(statusText(record.getStatus()));
        vo.setProcessedAt(record.getProcessedAt());
        vo.setProcessedBy(record.getProcessedBy());
        vo.setResultRemark(record.getResultRemark());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }

    private String statusText(Integer status) {
        return switch (status) {
            case 1 -> "pending";
            case 2 -> "approved";
            case 3 -> "rejected";
            default -> String.valueOf(status);
        };
    }

    private String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private String maskNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return "用户";
        }
        if (nickname.length() <= 2) {
            return nickname.charAt(0) + "*";
        }
        return nickname.charAt(0) + "**" + nickname.charAt(nickname.length() - 1);
    }

    private boolean isValidIdCard(String idCard) {
        if (!StringUtils.hasText(idCard)) {
            return false;
        }
        String value = idCard.trim().toUpperCase();
        if (value.length() != 18 || !value.matches("\\d{17}[\\dX]")) {
            return false;
        }
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checkCodes = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (value.charAt(i) - '0') * weights[i];
        }
        return value.charAt(17) == checkCodes[sum % 11];
    }

    private String generateBizNo() {
        return BIZ_NO_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
