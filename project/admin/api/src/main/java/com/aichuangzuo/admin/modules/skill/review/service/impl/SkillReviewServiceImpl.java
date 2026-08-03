package com.aichuangzuo.admin.modules.skill.review.service.impl;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.message.entity.MessageAggregate;
import com.aichuangzuo.admin.modules.message.mapper.MessageAggregateMapper;
import com.aichuangzuo.admin.modules.skill.entity.UserSkillAggregate;
import com.aichuangzuo.admin.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.admin.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.admin.modules.skill.review.dto.SkillReviewRow;
import com.aichuangzuo.admin.modules.skill.review.dto.request.SkillReviewPageRequest;
import com.aichuangzuo.admin.modules.skill.review.entity.AuditStatus;
import com.aichuangzuo.admin.modules.skill.review.enums.AdminSkillReviewErrorCode;
import com.aichuangzuo.admin.modules.skill.review.mapper.SkillReviewAggregateMapper;
import com.aichuangzuo.admin.modules.skill.review.mapper.SkillReviewMapper;
import com.aichuangzuo.admin.modules.skill.review.service.SkillReviewService;
import com.aichuangzuo.admin.modules.skill.review.vo.SkillReviewVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 风格审核服务实现。
 *
 * <p>int → string 翻译集中在 {@link #toVo}；状态机集中在 {@link #approve} / {@link #reject}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillReviewServiceImpl implements SkillReviewService {

    private static final int AUDIT_STATUS_APPROVED = 1;
    private static final int ENABLE_STATUS_ENABLED = 1;
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("0.20");

    private static final String MSG_TYPE_SKILL = "skill";
    private static final String SUB_TYPE_APPROVED = "approved";
    private static final String SUB_TYPE_REJECTED = "rejected";
    private static final int MSG_SCOPE_PERSONAL = 2;

    private final SkillReviewMapper skillReviewMapper;
    private final SkillReviewAggregateMapper aggregateMapper;
    private final SkillMarketMapper skillMarketMapper;
    private final MessageAggregateMapper messageAggregateMapper;

    @Override
    public IPage<SkillReviewVO> page(SkillReviewPageRequest request) {
        long offset = (long) (request.getPageNum() - 1) * request.getPageSize();
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;

        List<SkillReviewRow> rows = aggregateMapper.selectReviewPage(
                request.getStatus(), request.getReviewed(), keyword, offset, request.getPageSize());
        long total = aggregateMapper.countReviewPage(request.getStatus(), request.getReviewed(), keyword);

        List<SkillReviewVO> records = rows.stream().map(this::toVo).toList();

        Page<SkillReviewVO> page = new Page<>(request.getPageNum(), request.getPageSize());
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String bizNo) {
        UserSkillAggregate skill = loadByBizNo(bizNo);
        AuditStatus current = AuditStatus.of(skill.getAuditStatus());
        if (current == AuditStatus.APPROVED) {
            throw new BusinessException(AdminSkillReviewErrorCode.SKILL_REVIEW_ALREADY_APPROVED);
        }
        if (current == AuditStatus.REJECTED) {
            // v1：被打回后不允许再被通过，必须由用户重新提交。
            throw new BusinessException(AdminSkillReviewErrorCode.SKILL_REVIEW_ALREADY_REJECTED);
        }

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        skill.setAuditStatus(AuditStatus.APPROVED.getCode());
        skill.setAuditedBy(adminId);
        skill.setAuditedAt(LocalDateTime.now());
        skill.setRejectReason(null);
        skillReviewMapper.updateById(skill);
        syncToMarket(skill);
        pushSkillReviewMessage(skill, true, null);
        log.info("风格审核通过 bizNo={}, adminId={}", bizNo, adminId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchApprove(List<String> bizNos) {
        if (bizNos == null || bizNos.isEmpty()) {
            return 0;
        }
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        for (String bizNo : bizNos) {
            UserSkillAggregate skill = loadByBizNo(bizNo);
            AuditStatus current = AuditStatus.of(skill.getAuditStatus());
            if (current != AuditStatus.PENDING) {
                log.warn("批量通过跳过非待审核记录 bizNo={}, status={}", bizNo, current);
                continue;
            }
            skill.setAuditStatus(AuditStatus.APPROVED.getCode());
            skill.setAuditedBy(adminId);
            skill.setAuditedAt(now);
            skill.setRejectReason(null);
            skillReviewMapper.updateById(skill);
            syncToMarket(skill);
            pushSkillReviewMessage(skill, true, null);
            count++;
            log.info("批量风格审核通过 bizNo={}, adminId={}", bizNo, adminId);
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String bizNo, String reason) {
        if (!StringUtils.hasText(reason) || !StringUtils.hasText(reason.trim())) {
            throw new BusinessException(AdminSkillReviewErrorCode.REJECT_REASON_EMPTY);
        }
        UserSkillAggregate skill = loadByBizNo(bizNo);
        AuditStatus current = AuditStatus.of(skill.getAuditStatus());
        if (current == AuditStatus.APPROVED) {
            throw new BusinessException(AdminSkillReviewErrorCode.SKILL_REVIEW_ALREADY_APPROVED);
        }
        if (current == AuditStatus.REJECTED) {
            throw new BusinessException(AdminSkillReviewErrorCode.SKILL_REVIEW_ALREADY_REJECTED);
        }

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        skill.setAuditStatus(AuditStatus.REJECTED.getCode());
        skill.setAuditedBy(adminId);
        skill.setAuditedAt(LocalDateTime.now());
        skill.setRejectReason(reason.trim());
        skillReviewMapper.updateById(skill);
        pushSkillReviewMessage(skill, false, reason.trim());
        log.info("风格审核打回 bizNo={}, adminId={}, reason={}", bizNo, adminId, reason.trim());
    }

    /**
     * 按 bizNo 加载；找不到则抛 {@link AdminSkillReviewErrorCode#SKILL_REVIEW_NOT_FOUND}。
     */
    private UserSkillAggregate loadByBizNo(String bizNo) {
        LambdaQueryWrapper<UserSkillAggregate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkillAggregate::getBizNo, bizNo);
        UserSkillAggregate skill = skillReviewMapper.selectOne(wrapper);
        if (skill == null) {
            throw new BusinessException(AdminSkillReviewErrorCode.SKILL_REVIEW_NOT_FOUND);
        }
        return skill;
    }

    /**
     * 将审核通过的用户风格同步到风格市场表，供用户端市场列表查询。
     */
    private void syncToMarket(UserSkillAggregate skill) {
        LambdaQueryWrapper<SkillMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillMarket::getBizNo, skill.getBizNo())
                .eq(SkillMarket::getIsDeleted, 0);
        SkillMarket market = skillMarketMapper.selectOne(wrapper);

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (market != null) {
            market.setSkillName(skill.getSkillName());
            market.setDescription(skill.getDescription());
            market.setPromptSummary(skill.getPromptSummary());
            market.setPrompt(skill.getPrompt());
            market.setScope(skill.getScope());
            market.setPublisherUserId(skill.getUserId());
            market.setEnableStatus(ENABLE_STATUS_ENABLED);
            market.setAuditStatus(AUDIT_STATUS_APPROVED);
            market.setSourceType(skill.getSourceType());
            if (adminId != null) {
                market.setUpdatedBy(adminId);
            }
            skillMarketMapper.updateById(market);
            log.info("更新风格市场条目 bizNo={}", skill.getBizNo());
        } else {
            market = new SkillMarket();
            market.setBizNo(skill.getBizNo());
            market.setSkillName(skill.getSkillName());
            market.setDescription(skill.getDescription());
            market.setPromptSummary(skill.getPromptSummary());
            market.setPrompt(skill.getPrompt());
            market.setScope(skill.getScope());
            market.setPublisherUserId(skill.getUserId());
            market.setPrice(DEFAULT_PRICE);
            market.setTotalUses(0);
            market.setWeeklyUses(0);
            market.setWeeklyEarnings(BigDecimal.ZERO);
            market.setMilestoneBonus(BigDecimal.ZERO);
            market.setEnableStatus(ENABLE_STATUS_ENABLED);
            market.setAuditStatus(AUDIT_STATUS_APPROVED);
            market.setSourceType(skill.getSourceType());
            market.setIsDeleted(0);
            if (adminId != null) {
                market.setCreatedBy(adminId);
                market.setUpdatedBy(adminId);
            }
            skillMarketMapper.insert(market);
            log.info("创建风格市场条目 bizNo={}, name={}", skill.getBizNo(), skill.getSkillName());
        }
    }

    /**
     * 向用户推送风格审核结果消息。
     */
    private void pushSkillReviewMessage(UserSkillAggregate skill, boolean approved, String rejectReason) {
        String title = approved ? "提示词发布申请审核通过" : "提示词发布申请审核未通过";
        String summary = approved
                ? String.format("你的提示词「%s」已通过审核，已上架提示词市场。其他用户使用时，你将获得创作币收益。", skill.getSkillName())
                : String.format("你的提示词「%s」未通过审核，原因：%s", skill.getSkillName(), rejectReason);

        MessageAggregate message = new MessageAggregate();
        message.setMsgType(MSG_TYPE_SKILL);
        message.setScope(MSG_SCOPE_PERSONAL);
        message.setTargetUserId(skill.getUserId());
        message.setTitle(title);
        message.setSummary(summary);
        message.setContent(approved ? null : rejectReason);
        message.setSubType(approved ? SUB_TYPE_APPROVED : SUB_TYPE_REJECTED);
        message.setTenantId(0L);
        messageAggregateMapper.insert(message);
        log.info("推送风格审核消息 userId={}, bizNo={}, approved={}", skill.getUserId(), skill.getBizNo(), approved);
    }

    /**
     * 把 SQL 直出行翻译为前端契约 VO：{@code bizNo} → {@code id}、{@code skillName} → {@code name}、
     * int {@code sourceType} → "my"/"learned"、int {@code auditStatus} → "pending"/"approved"/"rejected"。
     */
    private SkillReviewVO toVo(SkillReviewRow row) {
        SkillReviewVO vo = new SkillReviewVO();
        vo.setId(row.getBizNo());
        vo.setName(row.getSkillName());
        vo.setSourceType(toSourceTypeString(row.getSourceType()));
        vo.setCreatorName(row.getCreatorName());
        vo.setPrompt(row.getPrompt());
        vo.setScope(row.getScope());
        vo.setStatus(toStatusString(row.getAuditStatus()));
        vo.setRejectReason(row.getRejectReason());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    private String toSourceTypeString(Integer code) {
        return code != null && code == 1 ? "my" : "learned";
    }

    private String toStatusString(Integer code) {
        if (code == null) {
            return "pending";
        }
        return switch (code) {
            case 1 -> "approved";
            case 2 -> "rejected";
            default -> "pending";
        };
    }
}
