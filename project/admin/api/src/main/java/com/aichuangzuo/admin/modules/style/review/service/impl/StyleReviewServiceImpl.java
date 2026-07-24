package com.aichuangzuo.admin.modules.style.review.service.impl;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.message.entity.MessageAggregate;
import com.aichuangzuo.admin.modules.message.mapper.MessageAggregateMapper;
import com.aichuangzuo.admin.modules.style.entity.UserStyleAggregate;
import com.aichuangzuo.admin.modules.style.market.entity.StyleMarket;
import com.aichuangzuo.admin.modules.style.market.mapper.StyleMarketMapper;
import com.aichuangzuo.admin.modules.style.review.dto.StyleReviewRow;
import com.aichuangzuo.admin.modules.style.review.dto.request.StyleReviewPageRequest;
import com.aichuangzuo.admin.modules.style.review.entity.AuditStatus;
import com.aichuangzuo.admin.modules.style.review.enums.AdminStyleReviewErrorCode;
import com.aichuangzuo.admin.modules.style.review.mapper.StyleReviewAggregateMapper;
import com.aichuangzuo.admin.modules.style.review.mapper.StyleReviewMapper;
import com.aichuangzuo.admin.modules.style.review.service.StyleReviewService;
import com.aichuangzuo.admin.modules.style.review.vo.StyleReviewVO;
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
public class StyleReviewServiceImpl implements StyleReviewService {

    private static final int AUDIT_STATUS_APPROVED = 1;
    private static final int ENABLE_STATUS_ENABLED = 1;
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("0.20");

    private static final String MSG_TYPE_STYLE = "style";
    private static final String SUB_TYPE_APPROVED = "approved";
    private static final String SUB_TYPE_REJECTED = "rejected";
    private static final int MSG_SCOPE_PERSONAL = 2;

    private final StyleReviewMapper styleReviewMapper;
    private final StyleReviewAggregateMapper aggregateMapper;
    private final StyleMarketMapper styleMarketMapper;
    private final MessageAggregateMapper messageAggregateMapper;

    @Override
    public IPage<StyleReviewVO> page(StyleReviewPageRequest request) {
        long offset = (long) (request.getPageNum() - 1) * request.getPageSize();
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;

        List<StyleReviewRow> rows = aggregateMapper.selectReviewPage(
                request.getStatus(), request.getReviewed(), keyword, offset, request.getPageSize());
        long total = aggregateMapper.countReviewPage(request.getStatus(), request.getReviewed(), keyword);

        List<StyleReviewVO> records = rows.stream().map(this::toVo).toList();

        Page<StyleReviewVO> page = new Page<>(request.getPageNum(), request.getPageSize());
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String bizNo) {
        UserStyleAggregate style = loadByBizNo(bizNo);
        AuditStatus current = AuditStatus.of(style.getAuditStatus());
        if (current == AuditStatus.APPROVED) {
            throw new BusinessException(AdminStyleReviewErrorCode.STYLE_REVIEW_ALREADY_APPROVED);
        }
        if (current == AuditStatus.REJECTED) {
            // v1：被打回后不允许再被通过，必须由用户重新提交。
            throw new BusinessException(AdminStyleReviewErrorCode.STYLE_REVIEW_ALREADY_REJECTED);
        }

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        style.setAuditStatus(AuditStatus.APPROVED.getCode());
        style.setAuditedBy(adminId);
        style.setAuditedAt(LocalDateTime.now());
        style.setRejectReason(null);
        styleReviewMapper.updateById(style);
        syncToMarket(style);
        pushStyleReviewMessage(style, true, null);
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
            UserStyleAggregate style = loadByBizNo(bizNo);
            AuditStatus current = AuditStatus.of(style.getAuditStatus());
            if (current != AuditStatus.PENDING) {
                log.warn("批量通过跳过非待审核记录 bizNo={}, status={}", bizNo, current);
                continue;
            }
            style.setAuditStatus(AuditStatus.APPROVED.getCode());
            style.setAuditedBy(adminId);
            style.setAuditedAt(now);
            style.setRejectReason(null);
            styleReviewMapper.updateById(style);
            syncToMarket(style);
            pushStyleReviewMessage(style, true, null);
            count++;
            log.info("批量风格审核通过 bizNo={}, adminId={}", bizNo, adminId);
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String bizNo, String reason) {
        if (!StringUtils.hasText(reason) || !StringUtils.hasText(reason.trim())) {
            throw new BusinessException(AdminStyleReviewErrorCode.REJECT_REASON_EMPTY);
        }
        UserStyleAggregate style = loadByBizNo(bizNo);
        AuditStatus current = AuditStatus.of(style.getAuditStatus());
        if (current == AuditStatus.APPROVED) {
            throw new BusinessException(AdminStyleReviewErrorCode.STYLE_REVIEW_ALREADY_APPROVED);
        }
        if (current == AuditStatus.REJECTED) {
            throw new BusinessException(AdminStyleReviewErrorCode.STYLE_REVIEW_ALREADY_REJECTED);
        }

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        style.setAuditStatus(AuditStatus.REJECTED.getCode());
        style.setAuditedBy(adminId);
        style.setAuditedAt(LocalDateTime.now());
        style.setRejectReason(reason.trim());
        styleReviewMapper.updateById(style);
        pushStyleReviewMessage(style, false, reason.trim());
        log.info("风格审核打回 bizNo={}, adminId={}, reason={}", bizNo, adminId, reason.trim());
    }

    /**
     * 按 bizNo 加载；找不到则抛 {@link AdminStyleReviewErrorCode#STYLE_REVIEW_NOT_FOUND}。
     */
    private UserStyleAggregate loadByBizNo(String bizNo) {
        LambdaQueryWrapper<UserStyleAggregate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStyleAggregate::getBizNo, bizNo);
        UserStyleAggregate style = styleReviewMapper.selectOne(wrapper);
        if (style == null) {
            throw new BusinessException(AdminStyleReviewErrorCode.STYLE_REVIEW_NOT_FOUND);
        }
        return style;
    }

    /**
     * 将审核通过的用户风格同步到风格市场表，供用户端市场列表查询。
     */
    private void syncToMarket(UserStyleAggregate style) {
        LambdaQueryWrapper<StyleMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StyleMarket::getBizNo, style.getBizNo())
                .eq(StyleMarket::getIsDeleted, 0);
        StyleMarket market = styleMarketMapper.selectOne(wrapper);

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (market != null) {
            market.setStyleName(style.getStyleName());
            market.setDescription(style.getDescription());
            market.setPromptSummary(style.getPromptSummary());
            market.setPrompt(style.getPrompt());
            market.setScope(style.getScope());
            market.setPublisherUserId(style.getUserId());
            market.setEnableStatus(ENABLE_STATUS_ENABLED);
            market.setAuditStatus(AUDIT_STATUS_APPROVED);
            market.setSourceType(style.getSourceType());
            if (adminId != null) {
                market.setUpdatedBy(adminId);
            }
            styleMarketMapper.updateById(market);
            log.info("更新风格市场条目 bizNo={}", style.getBizNo());
        } else {
            market = new StyleMarket();
            market.setBizNo(style.getBizNo());
            market.setStyleName(style.getStyleName());
            market.setDescription(style.getDescription());
            market.setPromptSummary(style.getPromptSummary());
            market.setPrompt(style.getPrompt());
            market.setScope(style.getScope());
            market.setPublisherUserId(style.getUserId());
            market.setPrice(DEFAULT_PRICE);
            market.setTotalUses(0);
            market.setWeeklyUses(0);
            market.setWeeklyEarnings(BigDecimal.ZERO);
            market.setMilestoneBonus(BigDecimal.ZERO);
            market.setEnableStatus(ENABLE_STATUS_ENABLED);
            market.setAuditStatus(AUDIT_STATUS_APPROVED);
            market.setSourceType(style.getSourceType());
            market.setIsDeleted(0);
            if (adminId != null) {
                market.setCreatedBy(adminId);
                market.setUpdatedBy(adminId);
            }
            styleMarketMapper.insert(market);
            log.info("创建风格市场条目 bizNo={}, name={}", style.getBizNo(), style.getStyleName());
        }
    }

    /**
     * 向用户推送风格审核结果消息。
     */
    private void pushStyleReviewMessage(UserStyleAggregate style, boolean approved, String rejectReason) {
        String title = approved ? "风格审核通过" : "风格审核未通过";
        String summary = approved
                ? String.format("你的风格「%s」已通过审核，已上架风格市场。其他用户使用时，你将获得创作币收益。", style.getStyleName())
                : String.format("你的风格「%s」未通过审核，原因：%s", style.getStyleName(), rejectReason);

        MessageAggregate message = new MessageAggregate();
        message.setMsgType(MSG_TYPE_STYLE);
        message.setScope(MSG_SCOPE_PERSONAL);
        message.setTargetUserId(style.getUserId());
        message.setTitle(title);
        message.setSummary(summary);
        message.setContent(approved ? null : rejectReason);
        message.setSubType(approved ? SUB_TYPE_APPROVED : SUB_TYPE_REJECTED);
        message.setTenantId(0L);
        messageAggregateMapper.insert(message);
        log.info("推送风格审核消息 userId={}, bizNo={}, approved={}", style.getUserId(), style.getBizNo(), approved);
    }

    /**
     * 把 SQL 直出行翻译为前端契约 VO：{@code bizNo} → {@code id}、{@code styleName} → {@code name}、
     * int {@code sourceType} → "my"/"learned"、int {@code auditStatus} → "pending"/"approved"/"rejected"。
     */
    private StyleReviewVO toVo(StyleReviewRow row) {
        StyleReviewVO vo = new StyleReviewVO();
        vo.setId(row.getBizNo());
        vo.setName(row.getStyleName());
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