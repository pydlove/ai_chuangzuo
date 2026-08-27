package com.aichuangzuo.user.modules.auth.service.impl;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.config.AuthProperties;
import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import com.aichuangzuo.user.infrastructure.security.JwtUtil;
import com.aichuangzuo.user.modules.auth.converter.AuthConverter;
import com.aichuangzuo.user.modules.auth.dto.request.QrLoginAuthorizeRequest;
import com.aichuangzuo.user.modules.auth.dto.request.QrLoginScanRequest;
import com.aichuangzuo.user.modules.auth.entity.QrLoginSession;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.enums.QrLoginStatus;
import com.aichuangzuo.user.modules.auth.mapper.QrLoginSessionMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.auth.service.QrLoginService;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;
import com.aichuangzuo.user.modules.auth.vo.QrLoginCreateVO;
import com.aichuangzuo.user.modules.auth.vo.QrLoginStatusVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrLoginServiceImpl implements QrLoginService {

    private static final int QR_CODE_LENGTH = 32;
    private static final int EXPIRE_MINUTES = 5;
    private static final String CACHE_KEY_PREFIX = "user:qr-login:session:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final QrLoginSessionMapper qrLoginSessionMapper;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final AuthConverter authConverter;
    private final AuthProperties authProperties;
    private final CacheUtil cacheUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QrLoginCreateVO create(String clientIp, String userAgent) {
        String qrCode = generateQrCode();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusMinutes(EXPIRE_MINUTES);

        QrLoginSession session = new QrLoginSession();
        session.setQrCode(qrCode);
        session.setStatus(QrLoginStatus.PENDING.getCode());
        session.setClientIp(clientIp);
        session.setUserAgent(userAgent);
        session.setExpiredAt(expiredAt);
        qrLoginSessionMapper.insert(session);

        cacheSession(session);

        QrLoginCreateVO vo = new QrLoginCreateVO();
        vo.setQrCode(qrCode);
        vo.setExpiresIn(EXPIRE_MINUTES * 60);
        return vo;
    }

    @Override
    public QrLoginStatusVO getStatus(String qrCode) {
        QrLoginSession session = getValidSession(qrCode);
        return toStatusVO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QrLoginStatusVO scan(QrLoginScanRequest request, Long scannerUserId) {
        QrLoginSession session = getValidSession(request.getQrCode());

        if (session.getStatus() != QrLoginStatus.PENDING.getCode()) {
            throw new BusinessException(UserAuthErrorCode.QR_LOGIN_STATUS_INVALID);
        }

        User scanner = userMapper.selectById(scannerUserId);
        if (scanner == null || scanner.getUserStatus() == 0) {
            throw new BusinessException(UserAuthErrorCode.ACCOUNT_DISABLED);
        }

        int rows = qrLoginSessionMapper.updateStatus(request.getQrCode(), QrLoginStatus.SCANNED.getCode(), QrLoginStatus.PENDING.getCode());
        if (rows == 0) {
            throw new BusinessException(UserAuthErrorCode.QR_LOGIN_STATUS_INVALID);
        }

        session.setStatus(QrLoginStatus.SCANNED.getCode());
        session.setScannerUserId(scannerUserId);
        session.setScannerNickname(scanner.getNickname());
        cacheSession(session);

        return toStatusVO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVO authorize(QrLoginAuthorizeRequest request, String clientIp, String userAgent) {
        QrLoginSession session = getValidSession(request.getQrCode());

        if (session.getStatus() != QrLoginStatus.SCANNED.getCode()) {
            throw new BusinessException(UserAuthErrorCode.QR_LOGIN_STATUS_INVALID);
        }

        Long userId = session.getScannerUserId();
        if (userId == null) {
            throw new BusinessException(UserAuthErrorCode.QR_LOGIN_STATUS_INVALID);
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getUserStatus() == 0) {
            throw new BusinessException(UserAuthErrorCode.ACCOUNT_DISABLED);
        }

        int rows = qrLoginSessionMapper.updateStatus(request.getQrCode(), QrLoginStatus.AUTHORIZED.getCode(), QrLoginStatus.SCANNED.getCode());
        if (rows == 0) {
            throw new BusinessException(UserAuthErrorCode.QR_LOGIN_STATUS_INVALID);
        }

        session.setStatus(QrLoginStatus.AUTHORIZED.getCode());
        cacheSession(session);

        AuthTokenVO vo = new AuthTokenVO();
        vo.setAccessToken(jwtUtil.generateAccessToken(user.getId()));
        vo.setRefreshToken(jwtUtil.generateRefreshToken(user.getId(), false));
        vo.setExpiresIn(Math.toIntExact(authProperties.getJwt().getAccessExpiration()));
        vo.setRememberMe(false);
        vo.setUser(authConverter.toUserVO(user));

        log.info("二维码登录授权成功, userId={}, qrCode={}, clientIp={}", userId, request.getQrCode(), clientIp);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String qrCode) {
        QrLoginSession session = getSessionFromCacheOrDb(qrCode);
        if (session == null) {
            throw new BusinessException(UserAuthErrorCode.QR_LOGIN_SESSION_NOT_FOUND);
        }

        if (session.getStatus() == QrLoginStatus.AUTHORIZED.getCode()
                || session.getStatus() == QrLoginStatus.EXPIRED.getCode()) {
            throw new BusinessException(UserAuthErrorCode.QR_LOGIN_SESSION_ALREADY_USED);
        }

        qrLoginSessionMapper.updateStatus(qrCode, QrLoginStatus.CANCELLED.getCode(), session.getStatus());
        session.setStatus(QrLoginStatus.CANCELLED.getCode());
        cacheSession(session);
    }

    private QrLoginSession getValidSession(String qrCode) {
        QrLoginSession session = getSessionFromCacheOrDb(qrCode);
        if (session == null) {
            throw new BusinessException(UserAuthErrorCode.QR_LOGIN_SESSION_NOT_FOUND);
        }

        if (session.getStatus() == QrLoginStatus.EXPIRED.getCode()) {
            throw new BusinessException(UserAuthErrorCode.QR_LOGIN_SESSION_EXPIRED);
        }

        if (session.getExpiredAt().isBefore(LocalDateTime.now())) {
            markExpired(session);
            throw new BusinessException(UserAuthErrorCode.QR_LOGIN_SESSION_EXPIRED);
        }

        if (session.getStatus() == QrLoginStatus.AUTHORIZED.getCode()
                || session.getStatus() == QrLoginStatus.CANCELLED.getCode()) {
            throw new BusinessException(UserAuthErrorCode.QR_LOGIN_SESSION_ALREADY_USED);
        }

        return session;
    }

    private QrLoginSession getSessionFromCacheOrDb(String qrCode) {
        String cacheKey = CACHE_KEY_PREFIX + qrCode;
        QrLoginSession cached = cacheUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        QrLoginSession session = qrLoginSessionMapper.selectOne(
                new LambdaQueryWrapper<QrLoginSession>()
                        .eq(QrLoginSession::getQrCode, qrCode)
                        .eq(QrLoginSession::getIsDeleted, 0)
        );
        if (session != null) {
            cacheSession(session);
        }
        return session;
    }

    private void cacheSession(QrLoginSession session) {
        long ttlMillis = ChronoUnit.MILLIS.between(LocalDateTime.now(), session.getExpiredAt());
        if (ttlMillis <= 0) {
            ttlMillis = 1;
        }
        cacheUtil.set(CACHE_KEY_PREFIX + session.getQrCode(), session, ttlMillis, TimeUnit.MILLISECONDS);
    }

    private void markExpired(QrLoginSession session) {
        qrLoginSessionMapper.updateStatus(session.getQrCode(), QrLoginStatus.EXPIRED.getCode(), session.getStatus());
        session.setStatus(QrLoginStatus.EXPIRED.getCode());
        cacheSession(session);
    }

    private QrLoginStatusVO toStatusVO(QrLoginSession session) {
        QrLoginStatusVO vo = new QrLoginStatusVO();
        vo.setQrCode(session.getQrCode());
        vo.setStatus(session.getStatus());
        QrLoginStatus status = QrLoginStatus.of(session.getStatus());
        vo.setStatusLabel(status != null ? status.getLabel() : "未知");
        vo.setScannerNickname(session.getScannerNickname());
        long remainingSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), session.getExpiredAt());
        vo.setExpiresIn((int) Math.max(0, remainingSeconds));
        return vo;
    }

    private String generateQrCode() {
        byte[] bytes = new byte[QR_CODE_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
