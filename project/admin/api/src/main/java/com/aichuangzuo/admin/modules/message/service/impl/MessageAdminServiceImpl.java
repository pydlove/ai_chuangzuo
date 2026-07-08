package com.aichuangzuo.admin.modules.message.service.impl;

import com.aichuangzuo.admin.modules.auth.entity.AdminUser;
import com.aichuangzuo.admin.modules.auth.mapper.AdminUserMapper;
import com.aichuangzuo.admin.modules.message.dto.request.MessageCreateRequest;
import com.aichuangzuo.admin.modules.message.dto.request.MessageQueryRequest;
import com.aichuangzuo.admin.modules.message.dto.request.MessageUpdateRequest;
import com.aichuangzuo.admin.modules.message.entity.MessageAggregate;
import com.aichuangzuo.admin.modules.message.enums.AdminMessageErrorCode;
import com.aichuangzuo.admin.modules.message.mapper.MessageAggregateMapper;
import com.aichuangzuo.admin.modules.message.service.MessageAdminService;
import com.aichuangzuo.admin.modules.message.vo.MessageAdminPageVO;
import com.aichuangzuo.admin.modules.message.vo.MessageAdminVO;
import com.aichuangzuo.admin.modules.message.vo.MessageAudienceVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 管理端-消息管理服务实现。
 *
 * <p>数据源：直接写用户表 {@code u_message}，复用 admin 端 MessageAggregate mapper。
 * 删除：业务上禁止删除（无对应接口），仍保留 {@code @TableLogic} 以兼容其他误调场景。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageAdminServiceImpl implements MessageAdminService {

    private static final Set<String> MANAGED_MSG_TYPES = Set.of("announcement", "feature", "promotion");
    private static final DateTimeFormatter CAMPAIGN_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MAX_PERSONAL_RECIPIENTS = 1000;

    private final MessageAggregateMapper messageAggregateMapper;
    private final AdminUserMapper adminUserMapper;

    @Override
    public MessageAdminPageVO list(MessageQueryRequest request) {
        validateMsgType(request.getMsgType());
        long offset = (long) (request.getPage() - 1) * request.getSize();
        String keyword = blankToNull(request.getKeyword());

        List<MessageAdminVO> rows = messageAggregateMapper.selectAdminPage(
                request.getMsgType(), keyword, offset, request.getSize());
        long total = messageAggregateMapper.countAdminPage(request.getMsgType(), keyword);

        long totalUsers = messageAggregateMapper.countAllActiveUsers();
        rows.forEach(vo -> decorate(vo, totalUsers));

        MessageAdminPageVO page = new MessageAdminPageVO();
        page.setList(rows);
        page.setTotal(total);
        return page;
    }

    @Override
    public MessageAdminVO detail(Long id) {
        MessageAggregate rep = findById(id);
        long totalUsers = messageAggregateMapper.countAllActiveUsers();
        MessageAdminVO vo = toVO(rep, totalUsers);
        if (vo.getScope() != null && vo.getScope() == 2) {
            List<MessageAudienceVO> audience = messageAggregateMapper.selectAudienceByBizNo(vo.getBizNo());
            audience.forEach(a -> a.setRead(a.getReadAt() != null));
            vo.setAudience(audience);
        } else {
            vo.setAudience(Collections.emptyList());
        }
        decorate(vo, totalUsers);
        return vo;
    }

    @Override
    @Transactional
    public MessageAdminVO create(MessageCreateRequest request) {
        validateMsgType(request.getMsgType());

        // 新功能强制全体
        if ("feature".equals(request.getMsgType())
                && request.getScope() != null && request.getScope() == 2) {
            throw new BusinessException(AdminMessageErrorCode.FEATURE_BROADCAST_ONLY);
        }
        int scope = "feature".equals(request.getMsgType()) ? 1 : request.getScope();

        List<Long> targetUserIds = request.getTargetUserIds();
        if (scope == 1) {
            targetUserIds = Collections.emptyList();
        } else {
            if (targetUserIds == null || targetUserIds.isEmpty()) {
                throw new BusinessException(AdminMessageErrorCode.TARGET_USERS_EMPTY);
            }
            if (targetUserIds.size() > MAX_PERSONAL_RECIPIENTS) {
                throw new BusinessException(AdminMessageErrorCode.TARGET_USERS_TOO_MANY);
            }
            targetUserIds = targetUserIds.stream().distinct().toList();
        }

        String campaignNo = generateCampaignNo();
        if (scope == 1) {
            insertOne(campaignNo, request.getMsgType(), 1, null,
                    request.getTitle(), request.getSummary(), request.getLinkUrl());
        } else {
            for (Long userId : targetUserIds) {
                insertOne(campaignNo, request.getMsgType(), 2, userId,
                        request.getTitle(), request.getSummary(), request.getLinkUrl());
            }
        }

        log.info("管理端发布消息 msgType={}, scope={}, campaignNo={}, recipients={}",
                request.getMsgType(), scope, campaignNo, targetUserIds.size());

        // 构造返回：取本次插入的代表行（max id）
        List<MessageAggregate> rows = messageAggregateMapper.selectByBizNo(campaignNo);
        MessageAggregate rep = rows.stream()
                .max((a, b) -> Long.compare(a.getId(), b.getId()))
                .orElseThrow(() -> new BusinessException(AdminMessageErrorCode.MESSAGE_NOT_FOUND));
        long totalUsers = messageAggregateMapper.countAllActiveUsers();
        return toVO(rep, totalUsers);
    }

    @Override
    @Transactional
    public MessageAdminVO update(Long id, MessageUpdateRequest request) {
        MessageAggregate rep = findById(id);
        // 仅改 title / summary / linkUrl；scope / targetUserIds / msgType / biz_no 不动
        LambdaUpdateWrapper<MessageAggregate> uw = Wrappers.lambdaUpdate(MessageAggregate.class)
                .eq(MessageAggregate::getBizNo, rep.getBizNo())
                .eq(MessageAggregate::getIsDeleted, 0)
                .set(MessageAggregate::getTitle, request.getTitle())
                .set(MessageAggregate::getSummary, request.getSummary())
                .set(MessageAggregate::getLinkUrl, request.getLinkUrl());
        messageAggregateMapper.update(null, uw);
        log.info("管理端更新消息 bizNo={}, id={}", rep.getBizNo(), id);

        MessageAggregate newest = messageAggregateMapper.selectByBizNo(rep.getBizNo()).stream()
                .max((a, b) -> Long.compare(a.getId(), b.getId()))
                .orElse(rep);
        long totalUsers = messageAggregateMapper.countAllActiveUsers();
        return toVO(newest, totalUsers);
    }

    // ---------------- helpers ----------------

    private MessageAggregate findById(Long id) {
        return messageAggregateMapper.selectById(id);
    }

    private void insertOne(String bizNo, String msgType, int scope, Long targetUserId,
                           String title, String summary, String linkUrl) {
        MessageAggregate m = new MessageAggregate();
        m.setBizNo(bizNo);
        m.setMsgType(msgType);
        m.setScope(scope);
        m.setTargetUserId(targetUserId);
        m.setTitle(title);
        m.setSummary(summary);
        m.setLinkUrl(linkUrl);
        m.setTenantId(0L);
        messageAggregateMapper.insert(m);
    }

    private MessageAdminVO toVO(MessageAggregate m, long totalUsers) {
        MessageAdminVO vo = new MessageAdminVO();
        vo.setId(m.getId());
        vo.setBizNo(m.getBizNo());
        vo.setMsgType(m.getMsgType());
        vo.setMsgTypeLabel(msgTypeLabel(m.getMsgType()));
        vo.setTitle(m.getTitle());
        vo.setSummary(m.getSummary());
        vo.setLinkUrl(m.getLinkUrl());
        vo.setScope(m.getScope());
        vo.setScopeLabel(m.getScope() != null && m.getScope() == 1 ? "全体" : "指定人");
        vo.setCreatedBy(m.getCreatedBy());
        vo.setCreatedAt(m.getCreatedAt());
        decorate(vo, totalUsers);
        return vo;
    }

    private void decorate(MessageAdminVO vo, long totalUsers) {
        vo.setMsgTypeLabel(msgTypeLabel(vo.getMsgType()));
        vo.setScopeLabel(vo.getScope() != null && vo.getScope() == 1 ? "全体" : "指定人");
        Long audience = vo.getAudienceCount();
        Long read = vo.getReadCount();
        if (vo.getScope() != null && vo.getScope() == 1) {
            // 全体：送达 = 总用户数
            vo.setAudienceCount(totalUsers);
            vo.setAudienceLabel("全体 " + totalUsers + " 人");
        } else {
            // 指定人：送达 = audienceCount（来自 COUNT(*) on biz_no）
            long a = audience == null ? 0 : audience;
            vo.setAudienceCount(a);
            vo.setAudienceLabel("指定 " + a + " 人");
        }
        long a = vo.getAudienceCount() == null ? 0 : vo.getAudienceCount();
        long r = read == null ? 0 : read;
        vo.setReadLabel(r + "/" + a);
        vo.setCreatedByName(resolveAdminName(vo.getCreatedBy()));
    }

    private String resolveAdminName(Long adminId) {
        if (adminId == null || adminId <= 0) return "-";
        AdminUser u = adminUserMapper.selectById(adminId);
        if (u == null) return "管理员#" + adminId;
        if (u.getRealName() != null && !u.getRealName().isBlank()) return u.getRealName();
        return u.getUsername();
    }

    private void validateMsgType(String msgType) {
        if (msgType == null || !MANAGED_MSG_TYPES.contains(msgType)) {
            throw new BusinessException(AdminMessageErrorCode.MESSAGE_TYPE_INVALID);
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String msgTypeLabel(String t) {
        if (t == null) return "";
        return switch (t) {
            case "announcement" -> "公告";
            case "feature" -> "新功能";
            case "promotion" -> "优惠活动";
            default -> t;
        };
    }

    private String generateCampaignNo() {
        return "MSG" + LocalDateTime.now().format(CAMPAIGN_TS)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
