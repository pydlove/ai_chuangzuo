package com.aichuangzuo.admin.modules.style.review.service.impl;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.style.review.dto.StyleReviewRow;
import com.aichuangzuo.admin.modules.style.review.dto.request.StyleReviewPageRequest;
import com.aichuangzuo.admin.modules.style.review.entity.AuditStatus;
import com.aichuangzuo.admin.modules.style.entity.UserStyleAggregate;
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

    private final StyleReviewMapper styleReviewMapper;
    private final StyleReviewAggregateMapper aggregateMapper;

    @Override
    public IPage<StyleReviewVO> page(StyleReviewPageRequest request) {
        long offset = (long) (request.getPageNum() - 1) * request.getPageSize();
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;

        List<StyleReviewRow> rows = aggregateMapper.selectReviewPage(
                request.getStatus(), keyword, offset, request.getPageSize());
        long total = aggregateMapper.countReviewPage(request.getStatus(), keyword);

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
        log.info("风格审核通过 bizNo={}, adminId={}", bizNo, adminId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String bizNo, String reason) {
        if (!StringUtils.hasText(reason) || !StringUtils.hasText(reason.trim())) {
            throw new BusinessException(AdminStyleReviewErrorCode.REJECT_REASON_EMPTY);
        }
        UserStyleAggregate style = loadByBizNo(bizNo);
        AuditStatus current = AuditStatus.of(style.getAuditStatus());
        if (current == AuditStatus.REJECTED) {
            throw new BusinessException(AdminStyleReviewErrorCode.STYLE_REVIEW_ALREADY_REJECTED);
        }

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        style.setAuditStatus(AuditStatus.REJECTED.getCode());
        style.setAuditedBy(adminId);
        style.setAuditedAt(LocalDateTime.now());
        style.setRejectReason(reason.trim());
        styleReviewMapper.updateById(style);
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