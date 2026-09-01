package com.aichuangzuo.admin.modules.skill.market.service.impl;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.earnings.vo.PageResult;
import com.aichuangzuo.admin.modules.skill.market.dto.SkillMarketOverviewDTO;
import com.aichuangzuo.admin.modules.skill.market.dto.SkillMarketRow;
import com.aichuangzuo.admin.modules.skill.market.dto.SkillMarketTopPublisherDTO;
import com.aichuangzuo.admin.modules.skill.market.dto.SkillMarketTopSkillDTO;
import com.aichuangzuo.admin.modules.skill.market.dto.SkillMarketTrendDTO;
import com.aichuangzuo.admin.modules.skill.market.dto.SkillMarketUsageRecordDTO;
import com.aichuangzuo.admin.modules.skill.market.dto.request.CreateSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.SkillMarketPageRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.UpdateSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.admin.modules.skill.market.enums.AdminSkillMarketErrorCode;
import com.aichuangzuo.admin.modules.skill.market.mapper.SkillMarketAggregateMapper;
import com.aichuangzuo.admin.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.admin.modules.skill.market.mapper.SkillMarketStatsMapper;
import com.aichuangzuo.admin.modules.skill.market.service.SkillMarketAdminService;
import com.aichuangzuo.admin.modules.skill.market.service.SkillMarketUsageClient;
import com.aichuangzuo.admin.modules.skill.market.vo.MarketSkillStatsVO;
import com.aichuangzuo.admin.modules.skill.market.vo.SkillMarketUsageRecordVO;
import com.aichuangzuo.admin.modules.skill.market.vo.SkillMarketVO;
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
public class SkillMarketAdminServiceImpl implements SkillMarketAdminService {

    private static final int AUDIT_STATUS_APPROVED = 1;
    private static final int SOURCE_TYPE_PLATFORM = 3;
    private static final java.math.BigDecimal DEFAULT_PRICE_PER_USE = new java.math.BigDecimal("2.00");

    private final SkillMarketMapper skillMarketMapper;
    private final SkillMarketAggregateMapper aggregateMapper;
    private final PlatformUserMapper platformUserMapper;
    private final SkillMarketStatsMapper statsMapper;
    private final SkillMarketUsageClient usageClient;

    @Override
    public IPage<SkillMarketVO> page(SkillMarketPageRequest request) {
        long offset = (long) (request.getPageNum() - 1) * request.getPageSize();
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : null;

        List<SkillMarketRow> rows = aggregateMapper.selectMarketStylePage(
                request.getEnableStatus(), request.getFeatured(), keyword, offset,
                request.getPageSize() != null ? request.getPageSize().longValue() : 20L);
        long total = aggregateMapper.countMarketStylePage(request.getEnableStatus(), request.getFeatured(), keyword);

        List<SkillMarketVO> records = rows.stream().map(this::toVo).toList();

        Page<SkillMarketVO> page = new Page<>(request.getPageNum(), request.getPageSize());
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CreateSkillMarketRequest request) {
        validateName(request.getSkillName());
        validateEnableStatus(request.getEnableStatus());
        validateFeatured(request.getFeatured());
        validatePublisher(request.getPublisherUserId());
        validateTotalUses(request.getTotalUses());

        SkillMarket market = new SkillMarket();
        market.setBizNo(generateBizNo());
        market.setSkillName(request.getSkillName().trim());
        market.setDescription(StringUtils.trimWhitespace(request.getDescription()));
        market.setPromptSummary(StringUtils.trimWhitespace(request.getPromptSummary()));
        market.setPrompt(request.getPrompt().trim());
        market.setScope(normalizeScope(request.getScope()));
        market.setPublisherUserId(request.getPublisherUserId());
        market.setTotalUses(request.getTotalUses() != null ? request.getTotalUses() : 0);
        market.setWeeklyUses(0);
        market.setPrice(DEFAULT_PRICE_PER_USE);
        market.setWeeklyEarnings(BigDecimal.ZERO);
        market.setMilestoneBonus(BigDecimal.ZERO);
        market.setEnableStatus(request.getEnableStatus());
        market.setFeatured(request.getFeatured());
        market.setAuditStatus(AUDIT_STATUS_APPROVED);
        market.setSourceType(SOURCE_TYPE_PLATFORM);
        market.setIsDeleted(0);

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId != null) {
            market.setCreatedBy(adminId);
            market.setUpdatedBy(adminId);
        }

        skillMarketMapper.insert(market);
        log.info("创建风格市场条目 bizNo={}, name={}", market.getBizNo(), market.getSkillName());
        return market.getBizNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String bizNo, UpdateSkillMarketRequest request) {
        validateEnableStatus(request.getEnableStatus());
        validateFeatured(request.getFeatured());
        validateTotalUses(request.getTotalUses());

        SkillMarket market = loadByBizNo(bizNo);

        String newName = request.getSkillName().trim();
        if (!newName.equals(market.getSkillName())) {
            ensureNameNotExists(newName, bizNo);
        }

        validatePublisher(request.getPublisherUserId());

        market.setSkillName(newName);
        market.setDescription(StringUtils.trimWhitespace(request.getDescription()));
        market.setPromptSummary(StringUtils.trimWhitespace(request.getPromptSummary()));
        market.setPrompt(request.getPrompt().trim());
        market.setScope(normalizeScope(request.getScope()));
        market.setPublisherUserId(request.getPublisherUserId());
        market.setTotalUses(request.getTotalUses() != null ? request.getTotalUses() : 0);
        market.setEnableStatus(request.getEnableStatus());
        market.setFeatured(request.getFeatured());

        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId != null) {
            market.setUpdatedBy(adminId);
        }

        skillMarketMapper.updateById(market);
        log.info("更新风格市场条目 bizNo={}, name={}, enableStatus={}",
                bizNo, newName, request.getEnableStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String bizNo) {
        SkillMarket market = loadByBizNo(bizNo);
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId != null) {
            market.setUpdatedBy(adminId);
        }
        skillMarketMapper.deleteById(market);
        log.info("软删风格市场条目 bizNo={}", bizNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(List<String> bizNos) {
        if (bizNos == null || bizNos.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<SkillMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SkillMarket::getBizNo, bizNos).eq(SkillMarket::getIsDeleted, 0);
        List<SkillMarket> markets = skillMarketMapper.selectList(wrapper);
        if (markets.isEmpty()) {
            return 0;
        }
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        for (SkillMarket market : markets) {
            if (adminId != null) {
                market.setUpdatedBy(adminId);
            }
            skillMarketMapper.deleteById(market);
        }
        log.info("管理员批量删除提示词市场条目, adminUserId={}, count={}", adminId, markets.size());
        return markets.size();
    }

    @Override
    public MarketSkillStatsVO stats() {
        SkillMarketOverviewDTO overview = statsMapper.selectOverview();
        List<SkillMarketTopSkillDTO> topSkills = statsMapper.selectTopSkillsByTotalUses(10);
        List<SkillMarketTopPublisherDTO> topPublishers = statsMapper.selectTopPublishersByWeeklyEarnings(10);
        List<SkillMarketTrendDTO> trends = statsMapper.selectUsageTrend(7);

        MarketSkillStatsVO vo = new MarketSkillStatsVO();
        vo.setOverview(toOverview(overview));
        vo.setTopSkills(topSkills.stream().map(this::toTopSkill).toList());
        vo.setTopPublishers(topPublishers.stream().map(this::toTopPublisher).toList());
        vo.setUsageTrend(trends.stream().map(this::toTrendItem).toList());
        return vo;
    }

    @Override
    public PageResult<SkillMarketUsageRecordVO> listUsageRecords(String bizNo, int pageNum, int pageSize) {
        long offset = (long) (pageNum - 1) * pageSize;
        long total = statsMapper.countUsageRecords(bizNo);
        List<SkillMarketUsageRecordDTO> rows = total == 0
                ? java.util.Collections.emptyList()
                : statsMapper.selectUsageRecords(bizNo, offset, pageSize);
        List<SkillMarketUsageRecordVO> items = rows.stream().map(this::toUsageRecord).toList();
        return new PageResult<>(items, total, pageNum, pageSize);
    }

    // -------- helpers --------
    @Override
    public void simulateUsage(String bizNo, Long userId) {
        SkillMarket market = loadByBizNo(bizNo);
        validateConsumer(userId);
        usageClient.recordUsage(bizNo, userId);
        log.info("管理员模拟使用提示词 bizNo={}, skillName={}, consumerUserId={}, adminUserId={}",
                bizNo, market.getSkillName(), userId, SecurityAdminContext.getCurrentAdminUserId());
    }


    private SkillMarket loadByBizNo(String bizNo) {
        LambdaQueryWrapper<SkillMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillMarket::getBizNo, bizNo)
                .eq(SkillMarket::getIsDeleted, 0);
        SkillMarket market = skillMarketMapper.selectOne(wrapper);
        if (market == null) {
            throw new BusinessException(AdminSkillMarketErrorCode.SKILL_MARKET_NOT_FOUND);
        }
        return market;
    }

    private void validateName(String name) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(name.trim())) {
            throw new BusinessException(AdminSkillMarketErrorCode.SKILL_MARKET_NAME_EXISTS);
        }
        ensureNameNotExists(name.trim(), null);
    }

    private void ensureNameNotExists(String name, String excludeBizNo) {
        LambdaQueryWrapper<SkillMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillMarket::getSkillName, name)
                .eq(SkillMarket::getIsDeleted, 0);
        if (excludeBizNo != null) {
            wrapper.ne(SkillMarket::getBizNo, excludeBizNo);
        }
        Long count = skillMarketMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(AdminSkillMarketErrorCode.SKILL_MARKET_NAME_EXISTS);
        }
    }

    private void validateEnableStatus(Integer enableStatus) {
        if (enableStatus == null || (enableStatus != 0 && enableStatus != 1)) {
            throw new BusinessException(AdminSkillMarketErrorCode.ENABLE_STATUS_INVALID);
        }
    }

    private void validateFeatured(Integer featured) {
        if (featured == null || (featured != 0 && featured != 1)) {
            throw new BusinessException(AdminSkillMarketErrorCode.FEATURED_STATUS_INVALID);
        }
    }

    private void validatePublisher(Long publisherUserId) {
        if (publisherUserId == null) {
            throw new BusinessException(AdminSkillMarketErrorCode.PUBLISHER_NOT_FOUND);
        }
        PlatformUser user = platformUserMapper.selectById(publisherUserId);
        if (user == null || user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            throw new BusinessException(AdminSkillMarketErrorCode.PUBLISHER_NOT_FOUND);
        }
    }

    private void validateTotalUses(Integer totalUses) {
        if (totalUses != null && totalUses < 0) {
            throw new BusinessException(AdminSkillMarketErrorCode.TOTAL_USES_INVALID);
        }
    }

   private String normalizeScope(String scope) {

        if (scope == null) {
            return null;
        }
        String trimmed = scope.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateConsumer(Long consumerUserId) {
        if (consumerUserId == null) {
            throw new BusinessException(AdminSkillMarketErrorCode.CONSUMER_USER_NOT_FOUND);
        }
        PlatformUser user = platformUserMapper.selectById(consumerUserId);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new BusinessException(AdminSkillMarketErrorCode.CONSUMER_USER_NOT_FOUND);
        }
    }

    private String generateBizNo() {
        return "SM" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private SkillMarketVO toVo(SkillMarketRow row) {
        SkillMarketVO vo = new SkillMarketVO();
        vo.setId(row.getBizNo());
        vo.setName(row.getSkillName());
        vo.setDescription(row.getDescription());
        vo.setPromptSummary(row.getPromptSummary());
        vo.setPrompt(row.getPrompt());
        vo.setScope(row.getScope());
        vo.setPublisherUserId(row.getPublisherUserId());
        vo.setPublisherName(row.getPublisherName());
        vo.setTotalUses(row.getTotalUses());
        vo.setWeeklyUses(row.getWeeklyUses());
        vo.setWeeklyEarnings(row.getWeeklyEarnings());
        vo.setMilestoneBonus(row.getMilestoneBonus());
        vo.setStatus(row.getEnableStatus() != null && row.getEnableStatus() == 1 ? "enabled" : "disabled");
        vo.setFeatured(row.getFeatured() != null && row.getFeatured() == 1 ? 1 : 0);
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    private MarketSkillStatsVO.Overview toOverview(SkillMarketOverviewDTO dto) {
        MarketSkillStatsVO.Overview vo = new MarketSkillStatsVO.Overview();
        if (dto == null) {
            return vo;
        }
        vo.setTotalSkills(dto.getTotalSkills());
        vo.setEnabledSkills(dto.getEnabledSkills());
        vo.setDisabledSkills(dto.getDisabledSkills());
        vo.setFeaturedSkills(dto.getFeaturedSkills());
        vo.setTotalUses(dto.getTotalUses());
        vo.setWeeklyUses(dto.getWeeklyUses());
        vo.setTotalEarnings(dto.getTotalEarnings());
        vo.setWeeklyEarnings(dto.getWeeklyEarnings());
        vo.setMilestoneBonus(dto.getMilestoneBonus());
        return vo;
    }

    private MarketSkillStatsVO.TopSkill toTopSkill(SkillMarketTopSkillDTO dto) {
        MarketSkillStatsVO.TopSkill vo = new MarketSkillStatsVO.TopSkill();
        vo.setSkillName(dto.getSkillName());
        vo.setPublisherName(dto.getPublisherName());
        vo.setPublisherUserId(dto.getPublisherUserId());
        vo.setTotalUses(dto.getTotalUses());
        vo.setWeeklyUses(dto.getWeeklyUses());
        vo.setWeeklyEarnings(dto.getWeeklyEarnings());
        vo.setMilestoneBonus(dto.getMilestoneBonus());
        return vo;
    }

    private MarketSkillStatsVO.TopPublisher toTopPublisher(SkillMarketTopPublisherDTO dto) {
        MarketSkillStatsVO.TopPublisher vo = new MarketSkillStatsVO.TopPublisher();
        vo.setPublisherUserId(dto.getPublisherUserId());
        vo.setPublisherName(dto.getPublisherName());
        vo.setSkillCount(dto.getSkillCount());
        vo.setTotalUses(dto.getTotalUses());
        vo.setWeeklyEarnings(dto.getWeeklyEarnings());
        return vo;
    }

    private MarketSkillStatsVO.TrendItem toTrendItem(SkillMarketTrendDTO dto) {
        MarketSkillStatsVO.TrendItem vo = new MarketSkillStatsVO.TrendItem();
        vo.setDate(dto.getDate());
        vo.setUses(dto.getUses());
        vo.setEarnings(dto.getEarnings());
        return vo;
    }

    private SkillMarketUsageRecordVO toUsageRecord(SkillMarketUsageRecordDTO dto) {
        SkillMarketUsageRecordVO vo = new SkillMarketUsageRecordVO();
        vo.setUserId(dto.getUserId());
        vo.setUserNickname(dto.getUserNickname());
        vo.setTaskBizNo(dto.getTaskBizNo());
        vo.setArticleBizNo(dto.getArticleBizNo());
        vo.setCompletedAt(dto.getCompletedAt());
        return vo;
    }
}
