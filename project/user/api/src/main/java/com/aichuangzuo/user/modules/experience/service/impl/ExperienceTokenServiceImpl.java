package com.aichuangzuo.user.modules.experience.service.impl;

import com.aichuangzuo.shared.entity.ExperienceToken;
import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.experience.mapper.ExperienceTokenMapper;
import com.aichuangzuo.user.modules.experience.service.ExperienceTokenService;
import com.aichuangzuo.user.modules.membership.service.MembershipService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceTokenServiceImpl implements ExperienceTokenService {

    private final ExperienceTokenMapper experienceTokenMapper;
    private final MembershipService membershipService;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consume(Long userId, String token) {
        ExperienceToken entity = experienceTokenMapper.selectOne(
                new LambdaQueryWrapper<ExperienceToken>()
                        .eq(ExperienceToken::getToken, token));
        if (entity == null) {
            throw new BusinessException(UserAuthErrorCode.EXPERIENCE_TOKEN_INVALID);
        }
        if (entity.getStatus() == 1) {
            throw new BusinessException(UserAuthErrorCode.EXPERIENCE_TOKEN_USED);
        }
        if (entity.getStatus() == 2) {
            throw new BusinessException(UserAuthErrorCode.EXPERIENCE_TOKEN_EXPIRED);
        }
        if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(UserAuthErrorCode.EXPERIENCE_TOKEN_EXPIRED);
        }

        membershipService.extendMembership(userId, entity.getPlanKey(), entity.getMembershipDays());

        User user = userMapper.selectById(userId);

        int affected = experienceTokenMapper.update(null,
                new LambdaUpdateWrapper<ExperienceToken>()
                        .eq(ExperienceToken::getId, entity.getId())
                        .eq(ExperienceToken::getStatus, 0)
                        .set(ExperienceToken::getStatus, 1)
                        .set(ExperienceToken::getUsedByUserId, userId)
                        .set(ExperienceToken::getUsedByUserName, user != null ? user.getNickname() : null)
                        .set(ExperienceToken::getUsedByUserEmail, user != null ? user.getEmail() : null)
                        .set(ExperienceToken::getUsedByUserPhone, user != null ? user.getPhone() : null)
                        .set(ExperienceToken::getUsedAt, LocalDateTime.now()));
        if (affected == 0) {
            throw new BusinessException(UserAuthErrorCode.EXPERIENCE_TOKEN_USED);
        }

        log.info("体验令牌消费成功 userId={}, token={}", userId, token);
    }
}
