package com.aichuangzuo.admin.modules.style.market.service.impl;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.style.market.dto.StyleMarketRow;
import com.aichuangzuo.admin.modules.style.market.dto.request.CreateStyleMarketRequest;
import com.aichuangzuo.admin.modules.style.market.dto.request.StyleMarketPageRequest;
import com.aichuangzuo.admin.modules.style.market.dto.request.UpdateStyleMarketRequest;
import com.aichuangzuo.admin.modules.style.market.entity.StyleMarket;
import com.aichuangzuo.admin.modules.style.market.enums.AdminStyleMarketErrorCode;
import com.aichuangzuo.admin.modules.style.market.mapper.StyleMarketAggregateMapper;
import com.aichuangzuo.admin.modules.style.market.mapper.StyleMarketMapper;
import com.aichuangzuo.admin.modules.style.market.service.StyleMarketAdminService;
import com.aichuangzuo.admin.modules.style.market.vo.StyleMarketVO;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
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
import java.util.List;
import java.util.UUID;

/**
 * 管理端 - 风格市场服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StyleMarketAdminServiceImpl implements StyleMarketAdminService {

    private static final int AUDIT_STATUS_APPROVED = 1;
    private static final int SOURCE_TYPE_PLATFORM = 3;
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("0.20");

    private final StyleMarketMapper styleMarketMapper;
    private final StyleMarketAggregateMapper aggregateMapper;
    private final PlatformUserMapper platformUserMapper;

    @Override
    public IPage<StyleMarketVO> page(StyleMarketPageRequest request) {
        long offset = (long) (request.getPageNum() - 1) * request.getPageSize();
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;

        List<StyleMarketRow> rows = aggregateMapper.selectMarketStylePage(
                request.getEnableStatus(), keyword, offset, request.getPageSize());
        long total = aggregateMapper.countMarketStylePage(request.getEnableStatus(), keyword);

        List<StyleMarketVO> records = rows.stream().map(this::toVo).toList();

        Page<StyleMarketVO> page = new Page<>(request.getPageNum(), request.getPageSize());
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CreateStyleMarketRequest request) {
        validateName(request.getStyleName());
        validateEnableStatus(request.getEnableStatus());
        validatePublisher(request.getPublisherUserId());
        validateTotalUses(request.getTotalUses());

        StyleMarket market = new StyleMarket();
        market.setBizNo(generateBizNo());
        market.setStyleName(request.getStyleName().trim());
        market.setDescription(StringUtils.trimWhitespace(request.getDescription()));
        market.setPromptSummary(StringUtils.trimWhitespace(request.getPromptSummary()));
        market.setPrompt(request.getPrompt().trim());
        market.setScope(normalizeScope(request.getScope()));
        market.setPublisherUserId(request.getPublisherUserId());
        market.setPrice(DEFAULT_PRICE);
        market.setTotalUses(request.getTotalUses() != null ? request.getTotalUses() : 0);
        market.setWeeklyUses(0);
        market.setWeeklyEarnings(BigDecimal.ZERO);
        market.setMilestoneBonus(BigDecimal.ZERO);
        market.setEnableStatus(request.getEnableStatus());
        market.setAuditStatus(AUDIT_STATUS_APPROVED);
        market.setSourceType(SOURCE_TYPE_PLATFORM);
        market.setIsDeleted(0);

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId != null) {
            market.setCreatedBy(adminId);
            market.setUpdatedBy(adminId);
        }

        styleMarketMapper.insert(market);
        log.info("创建风格市场条目 bizNo={}, name={}", market.getBizNo(), market.getStyleName());
        return market.getBizNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String bizNo, UpdateStyleMarketRequest request) {
        validateEnableStatus(request.getEnableStatus());
        validateTotalUses(request.getTotalUses());

        StyleMarket market = loadByBizNo(bizNo);

        String newName = request.getStyleName().trim();
        if (!newName.equals(market.getStyleName())) {
            ensureNameNotExists(newName, bizNo);
        }

        validatePublisher(request.getPublisherUserId());

        market.setStyleName(newName);
        market.setDescription(StringUtils.trimWhitespace(request.getDescription()));
        market.setPromptSummary(StringUtils.trimWhitespace(request.getPromptSummary()));
        market.setPrompt(request.getPrompt().trim());
        market.setScope(normalizeScope(request.getScope()));
        market.setPublisherUserId(request.getPublisherUserId());
        market.setTotalUses(request.getTotalUses() != null ? request.getTotalUses() : 0);
        market.setEnableStatus(request.getEnableStatus());

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId != null) {
            market.setUpdatedBy(adminId);
        }

        styleMarketMapper.updateById(market);
        log.info("更新风格市场条目 bizNo={}, name={}, enableStatus={}",
                bizNo, newName, request.getEnableStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String bizNo) {
        StyleMarket market = loadByBizNo(bizNo);
        market.setIsDeleted(1);
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId != null) {
            market.setUpdatedBy(adminId);
        }
        styleMarketMapper.updateById(market);
        log.info("软删风格市场条目 bizNo={}", bizNo);
    }

    // -------- helpers --------

    private StyleMarket loadByBizNo(String bizNo) {
        LambdaQueryWrapper<StyleMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StyleMarket::getBizNo, bizNo)
                .eq(StyleMarket::getIsDeleted, 0);
        StyleMarket market = styleMarketMapper.selectOne(wrapper);
        if (market == null) {
            throw new BusinessException(AdminStyleMarketErrorCode.STYLE_MARKET_NOT_FOUND);
        }
        return market;
    }

    private void validateName(String name) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(name.trim())) {
            throw new BusinessException(AdminStyleMarketErrorCode.STYLE_MARKET_NAME_EXISTS);
        }
        ensureNameNotExists(name.trim(), null);
    }

    private void ensureNameNotExists(String name, String excludeBizNo) {
        LambdaQueryWrapper<StyleMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StyleMarket::getStyleName, name)
                .eq(StyleMarket::getIsDeleted, 0);
        if (excludeBizNo != null) {
            wrapper.ne(StyleMarket::getBizNo, excludeBizNo);
        }
        Long count = styleMarketMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(AdminStyleMarketErrorCode.STYLE_MARKET_NAME_EXISTS);
        }
    }

    private void validateEnableStatus(Integer enableStatus) {
        if (enableStatus == null || (enableStatus != 0 && enableStatus != 1)) {
            throw new BusinessException(AdminStyleMarketErrorCode.ENABLE_STATUS_INVALID);
        }
    }

    private void validatePublisher(Long publisherUserId) {
        if (publisherUserId == null) {
            throw new BusinessException(AdminStyleMarketErrorCode.PUBLISHER_NOT_FOUND);
        }
        PlatformUser user = platformUserMapper.selectById(publisherUserId);
        if (user == null || user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            throw new BusinessException(AdminStyleMarketErrorCode.PUBLISHER_NOT_FOUND);
        }
    }

    private void validateTotalUses(Integer totalUses) {
        if (totalUses != null && totalUses < 0) {
            throw new BusinessException(AdminStyleMarketErrorCode.TOTAL_USES_INVALID);
        }
    }

    private String normalizeScope(String scope) {
        if (scope == null) {
            return null;
        }
        String trimmed = scope.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateBizNo() {
        return "SM" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private StyleMarketVO toVo(StyleMarketRow row) {
        StyleMarketVO vo = new StyleMarketVO();
        vo.setId(row.getBizNo());
        vo.setName(row.getStyleName());
        vo.setDescription(row.getDescription());
        vo.setPromptSummary(row.getPromptSummary());
        vo.setPrompt(row.getPrompt());
        vo.setScope(row.getScope());
        vo.setPublisherUserId(row.getPublisherUserId());
        vo.setPublisherName(row.getPublisherName());
        vo.setPrice(row.getPrice());
        vo.setTotalUses(row.getTotalUses());
        vo.setWeeklyUses(row.getWeeklyUses());
        vo.setWeeklyEarnings(row.getWeeklyEarnings());
        vo.setMilestoneBonus(row.getMilestoneBonus());
        vo.setStatus(row.getEnableStatus() != null && row.getEnableStatus() == 1 ? "enabled" : "disabled");
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }
}
