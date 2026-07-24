package com.aichuangzuo.user.modules.user.service.impl;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.user.dto.request.BindInviteCodeRequest;
import com.aichuangzuo.user.modules.user.service.InviteRewardService;
import com.aichuangzuo.user.modules.user.service.UserInviteBindingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 邀请人绑定服务实现。
 *
 * <p>校验规则：
 * <ol>
 *   <li>用户已存在且未绑定过邀请人</li>
 *   <li>注册 7 天内可补绑</li>
 *   <li>邀请码有效且指向真实用户</li>
 *   <li>不能自己绑自己</li>
 *   <li>不能形成循环邀请关系（沿邀请链向上查）</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInviteBindingServiceImpl implements UserInviteBindingService {

    /** 补绑邀请人的有效时间窗口：注册后 7 天。 */
    private static final int BINDING_WINDOW_DAYS = 7;

    private final UserMapper userMapper;
    private final UserInviteRelationMapper userInviteRelationMapper;
    private final InviteRewardService inviteRewardService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindInviteCode(BindInviteCodeRequest request) {
        Long currentUserId = SecurityUserContext.getCurrentUserId();
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException(UserAuthErrorCode.USER_NOT_FOUND);
        }

        if (userInviteRelationMapper.selectByInviteeId(currentUserId) != null) {
            throw new BusinessException(UserAuthErrorCode.INVITE_ALREADY_BOUND);
        }

        if (currentUser.getCreatedAt() == null
                || currentUser.getCreatedAt().plusDays(BINDING_WINDOW_DAYS).isBefore(LocalDateTime.now())) {
            throw new BusinessException(UserAuthErrorCode.INVITE_BINDING_EXPIRED);
        }

        String code = request.getInviteCode().trim().toUpperCase();
        User inviter = userMapper.selectByInviteCode(code);
        if (inviter == null) {
            throw new BusinessException(UserAuthErrorCode.INVITE_CODE_INVALID);
        }

        if (inviter.getId().equals(currentUserId)) {
            throw new BusinessException(UserAuthErrorCode.INVITE_SELF_NOT_ALLOWED);
        }

        if (wouldFormCircularRelation(currentUserId, inviter.getId())) {
            throw new BusinessException(UserAuthErrorCode.INVITE_CIRCULAR_NOT_ALLOWED);
        }

        UserInviteRelation relation = new UserInviteRelation();
        relation.setInviterId(inviter.getId());
        relation.setInviteeId(currentUserId);
        relation.setInviteCode(code);
        relation.setSourceType(2); // 手动填写
        relation.setEffectiveStatus(1); // 立即生效
        userInviteRelationMapper.insert(relation);

        inviteRewardService.rewardAfterBinding(currentUser, inviter);

        log.info("用户 {} 补绑邀请人 {}，inviteCode={}", currentUserId, inviter.getId(), code);
    }

    /**
     * 检查把 inviterId 设为 currentUserId 的邀请人是否会形成循环。
     *
     * <p>沿 inviterId 的邀请链向上追溯，如果链中出现 currentUserId，说明 currentUserId
     * 已经是 inviterId 的上游邀请人，再反向绑定会形成循环。
     *
     * @param currentUserId 当前用户 ID
     * @param inviterId     拟绑定的邀请人 ID
     * @return true 表示会形成循环
     */
    private boolean wouldFormCircularRelation(Long currentUserId, Long inviterId) {
        Long ancestorId = inviterId;
        int depth = 0;
        // 邀请链深度兜底，防止异常数据导致死循环
        final int maxDepth = 100;
        while (ancestorId != null && depth < maxDepth) {
            UserInviteRelation relation = userInviteRelationMapper.selectByInviteeId(ancestorId);
            if (relation == null) {
                return false;
            }
            if (relation.getInviterId().equals(currentUserId)) {
                return true;
            }
            ancestorId = relation.getInviterId();
            depth++;
        }
        return false;
    }
}
