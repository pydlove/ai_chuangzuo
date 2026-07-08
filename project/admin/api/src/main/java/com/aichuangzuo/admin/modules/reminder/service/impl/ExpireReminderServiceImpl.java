package com.aichuangzuo.admin.modules.reminder.service.impl;

import com.aichuangzuo.admin.modules.reminder.dto.request.ExpiringUserPageQuery;
import com.aichuangzuo.admin.modules.reminder.entity.Message;
import com.aichuangzuo.admin.modules.reminder.entity.ReminderConfig;
import com.aichuangzuo.admin.modules.reminder.entity.ReminderSendLog;
import com.aichuangzuo.admin.modules.reminder.enums.AdminReminderErrorCode;
import com.aichuangzuo.admin.modules.reminder.mapper.MessageMapper;
import com.aichuangzuo.admin.modules.reminder.mapper.ReminderConfigMapper;
import com.aichuangzuo.admin.modules.reminder.mapper.ReminderSendLogMapper;
import com.aichuangzuo.admin.modules.reminder.service.ExpireReminderService;
import com.aichuangzuo.admin.modules.reminder.service.ReminderConfigService;
import com.aichuangzuo.admin.modules.reminder.service.ReminderMailService;
import com.aichuangzuo.admin.modules.reminder.vo.ExpiringUserVO;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpireReminderServiceImpl implements ExpireReminderService {

    private final PlatformUserMapper userMapper;
    private final ReminderConfigMapper configMapper;
    private final ReminderSendLogMapper sendLogMapper;
    private final MessageMapper messageMapper;
    private final ReminderConfigService configService;
    private final ReminderMailService mailService;

    /**
     * 剩余天数（不含今天）。
     * expireAt 存的是"到期日次日 00:00" → lastValidDate = expireAt.toLocalDate() - 1 day。
     * 命中区间：0 ≤ remainingDays ≤ advanceDays。
     */
    @Override
    public int calcRemainingDays(LocalDateTime expireAt) {
        return calcRemainingDays(expireAt, LocalDate.now());
    }

    /** 同上，便于测试时注入 today。 */
    public int calcRemainingDays(LocalDateTime expireAt, LocalDate today) {
        if (expireAt == null) return Integer.MIN_VALUE;
        LocalDate lastValidDate = expireAt.toLocalDate().minusDays(1);
        return (int) ChronoUnit.DAYS.between(today, lastValidDate);
    }

    @Override
    public PageResult pageExpiringUsers(ExpiringUserPageQuery query) {
        int advanceDays = query.getAdvanceDays() == null
                ? configService.getConfig().getAdvanceDays()
                : query.getAdvanceDays();
        long pageNum = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long pageSize = query.getSize() == null || query.getSize() < 1 ? 20 : query.getSize();

        LocalDate today = LocalDate.now();
        LocalDateTime lower = today.atStartOfDay();
        LocalDateTime upper = today.plusDays(advanceDays + 1L).atStartOfDay();

        LambdaQueryWrapper<PlatformUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(PlatformUser::getMembershipExpireAt)
                .eq(PlatformUser::getIsDeleted, 0)
                .gt(PlatformUser::getMembershipExpireAt, lower)
                .le(PlatformUser::getMembershipExpireAt, upper);

        Page<PlatformUser> page = userMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        List<ExpiringUserVO> items = new ArrayList<>();
        for (PlatformUser u : page.getRecords()) {
            int rd = calcRemainingDays(u.getMembershipExpireAt(), today);
            if (rd < 0 || rd > advanceDays) continue;
            ExpiringUserVO vo = new ExpiringUserVO();
            vo.setUserId(u.getId());
            vo.setEmail(u.getEmail());
            vo.setNickname(u.getNickname());
            vo.setMembershipExpireAt(u.getMembershipExpireAt());
            vo.setRemainingDays(rd);
            ReminderSendLog log = sendLogMapper.selectOne(new LambdaQueryWrapper<ReminderSendLog>()
                    .eq(ReminderSendLog::getUserId, u.getId())
                    .eq(ReminderSendLog::getStatus, 1)
                    .orderByDesc(ReminderSendLog::getCreatedAt)
                    .last("LIMIT 1"));
            if (log != null) {
                vo.setLastRemindedAt(log.getCreatedAt());
                vo.setLastReminderChannel(log.getChannel());
            }
            items.add(vo);
        }
        items.sort((a, b) -> Integer.compare(a.getRemainingDays(), b.getRemainingDays()));
        return new PageResult(items, page.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RemindResult remindUser(Long userId, String triggerType) {
        PlatformUser user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1 || user.getMembershipExpireAt() == null) {
            throw new BusinessException(AdminReminderErrorCode.TARGET_USER_NOT_FOUND);
        }

        ReminderConfig cfg = configService.getConfig();
        int remainingDays = calcRemainingDays(user.getMembershipExpireAt());
        LocalDate today = LocalDate.now();
        String bizNo = "RMD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        List<String> sentChannels = new ArrayList<>();
        String channel = cfg.getNotifyChannel();
        if ("message".equals(channel) || "message_email".equals(channel)) {
            if (sendMessage(user, remainingDays, bizNo, today, triggerType)) {
                sentChannels.add("message");
            }
        }
        if ("email".equals(channel) || "message_email".equals(channel)) {
            if (sendEmail(user, remainingDays, today, triggerType)) {
                sentChannels.add("email");
            }
        }
        return new RemindResult(userId, remainingDays, sentChannels);
    }

    private boolean sendMessage(PlatformUser user, int remainingDays, String bizNo,
                                LocalDate today, String triggerType) {
        if (alreadySent(user.getId(), "message", today)) {
            log.info("用户 {} 今日 message 已发送，跳过", user.getId());
            return false;
        }
        String title = "您的会员即将到期";
        String summary = remainingDays == 0
                ? "您的会员将于今天到期，请及时续费"
                : "您的会员将于 " + remainingDays + " 天后到期，请及时续费";
        Message msg = new Message();
        msg.setBizNo(bizNo);
        msg.setMsgType("membership");
        msg.setSubType("membership.expiring");
        msg.setScope(2);
        msg.setTargetUserId(user.getId());
        msg.setTitle(title);
        msg.setSummary(summary);
        msg.setContent(summary + "\n\n点击「我的会员」查看详情并续费。");
        msg.setLinkUrl("/me/membership");
        msg.setTenantId(0L);
        msg.setIsDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        msg.setCreatedAt(now);
        msg.setUpdatedAt(now);
        msg.setCreatedBy(0L);
        msg.setUpdatedBy(0L);
        messageMapper.insert(msg);
        recordLog(user.getId(), "message", today, remainingDays, triggerType, 1, null);
        return true;
    }

    private boolean sendEmail(PlatformUser user, int remainingDays, LocalDate today, String triggerType) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("用户 {} 无邮箱，跳过邮件提醒", user.getId());
            return false;
        }
        if (alreadySent(user.getId(), "email", today)) {
            log.info("用户 {} 今日 email 已发送，跳过", user.getId());
            return false;
        }
        String subject = "您的爱创作会员即将到期";
        String text = "您好，" + (user.getNickname() == null ? "" : user.getNickname()) + "：\n\n"
                + (remainingDays == 0
                    ? "您的会员将于今天到期，请及时续费以免影响使用。"
                    : "您的会员将于 " + remainingDays + " 天后到期，请及时续费以免影响使用。")
                + "\n\n登录爱创作：https://aichuangzuo.com\n"
                + "本邮件由系统自动发出，请勿直接回复。";
        try {
            mailService.send(user.getEmail(), subject, text);
            recordLog(user.getId(), "email", today, remainingDays, triggerType, 1, null);
            return true;
        } catch (Exception ex) {
            recordLog(user.getId(), "email", today, remainingDays, triggerType, 0, ex.getMessage());
            return false;
        }
    }

    private boolean alreadySent(Long userId, String channel, LocalDate today) {
        Long count = sendLogMapper.selectCount(new LambdaQueryWrapper<ReminderSendLog>()
                .eq(ReminderSendLog::getUserId, userId)
                .eq(ReminderSendLog::getChannel, channel)
                .eq(ReminderSendLog::getSendDate, today)
                .eq(ReminderSendLog::getStatus, 1));
        return count != null && count > 0;
    }

    private void recordLog(Long userId, String channel, LocalDate today, int remainingDays,
                           String triggerType, int status, String failReason) {
        ReminderSendLog log = new ReminderSendLog();
        log.setUserId(userId);
        log.setChannel(channel);
        log.setSendDate(today);
        log.setRemainingDays(remainingDays);
        log.setTriggerType(triggerType);
        log.setStatus(status);
        log.setFailReason(failReason);
        log.setCreatedAt(LocalDateTime.now());
        sendLogMapper.insert(log);
    }
}