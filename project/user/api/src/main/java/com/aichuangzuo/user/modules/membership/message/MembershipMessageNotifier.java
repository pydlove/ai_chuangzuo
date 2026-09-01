package com.aichuangzuo.user.modules.membership.message;

import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import com.aichuangzuo.user.modules.membership.service.PlanLookupService;
import com.aichuangzuo.user.modules.message.enums.MessageSubType;
import com.aichuangzuo.user.modules.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * 会员订阅消息通知器。
 */
@Component
@RequiredArgsConstructor
public class MembershipMessageNotifier {

    private final MessageService messageService;
    private final PlanLookupService planLookupService;

    public void sendSubscriptionNotification(Long userId, MembershipPlan plan, UserMembership membership) {
        String levelName = planLookupService.getDisplayName(plan.getKey());
        String expiresAt = membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String summary = String.format("您已成功开通 %s，有效期至 %s", levelName, expiresAt);
        String content = String.format(
                "亲爱的用户：\n\n您的 %s 会员已成功开通，有效期至 %s。\n\n感谢您对爱创作的支持！",
                levelName, expiresAt);

        messageService.pushPersonal(
                userId,
                "membership",
                "订阅成功",
                summary,
                null,
                content,
                MessageSubType.SUBSCRIBED.getCode());
    }
}
