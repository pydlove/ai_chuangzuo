package com.aichuangzuo.admin.modules.hotsearch.service.impl;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchDailyQueryRequest;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchDailyRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchDaily;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.enums.AdminHotSearchErrorCode;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchPlatformMapper;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchDailyAdminService;
import com.aichuangzuo.admin.modules.hotsearch.vo.HotSearchDailyAdminVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotSearchDailyAdminServiceImpl implements HotSearchDailyAdminService {

    private final HotSearchDailyMapper dailyMapper;
    private final HotSearchPlatformMapper platformMapper;

    @Override
    public PageResult list(HotSearchDailyQueryRequest req) {
        LambdaQueryWrapper<HotSearchDaily> wrapper = new LambdaQueryWrapper<>();
        if (req.getPlatform() != null && !req.getPlatform().isBlank()) {
            wrapper.eq(HotSearchDaily::getPlatformCode, req.getPlatform());
        }
        if (req.getDate() != null) {
            wrapper.eq(HotSearchDaily::getSnapshotDate, req.getDate());
        }
        wrapper.orderByDesc(HotSearchDaily::getSnapshotDate)
                .orderByAsc(HotSearchDaily::getPlatformCode)
                .orderByAsc(HotSearchDaily::getRankNum);

        Page<HotSearchDaily> page = dailyMapper.selectPage(
                Page.of(req.getPage(), req.getSize()), wrapper);

        Map<String, String> platformNames = platformMapper.selectList(null).stream()
                .collect(Collectors.toMap(HotSearchPlatform::getCode, HotSearchPlatform::getName, (a, b) -> a));
        List<HotSearchDailyAdminVO> items = page.getRecords().stream().map(d -> toVO(d, platformNames)).toList();
        return new PageResult(items, page.getTotal(), req.getPage(), req.getSize());
    }

    @Override
    public HotSearchDailyAdminVO get(Long id) {
        HotSearchDaily d = dailyMapper.selectById(id);
        if (d == null) throw new BusinessException(AdminHotSearchErrorCode.DAILY_NOT_FOUND);
        return toVO(d, platformNameMap());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HotSearchDailyAdminVO create(HotSearchDailyRequest req) {
        // 校验平台存在
        HotSearchPlatform p = platformMapper.selectOne(
                new LambdaQueryWrapper<HotSearchPlatform>().eq(HotSearchPlatform::getCode, req.getPlatformCode()));
        if (p == null) throw new BusinessException(AdminHotSearchErrorCode.PLATFORM_NOT_FOUND);

        // 校验 (platformCode, snapshotDate, rankNum) 唯一
        Long dup = dailyMapper.selectCount(new LambdaQueryWrapper<HotSearchDaily>()
                .eq(HotSearchDaily::getPlatformCode, req.getPlatformCode())
                .eq(HotSearchDaily::getSnapshotDate, req.getSnapshotDate())
                .eq(HotSearchDaily::getRankNum, req.getRankNum()));
        if (dup != null && dup > 0) {
            throw new BusinessException(AdminHotSearchErrorCode.DAILY_RANK_DUPLICATED);
        }

        HotSearchDaily d = new HotSearchDaily();
        d.setPlatformCode(req.getPlatformCode());
        d.setRankNum(req.getRankNum());
        d.setTitle(req.getTitle());
        d.setHotValue(req.getHotValue());
        d.setUrl(req.getUrl());
        d.setSearchCount(req.getSearchCount());
        d.setSnapshotDate(req.getSnapshotDate());
        d.setCreatedAt(LocalDateTime.now());
        dailyMapper.insert(d);
        return toVO(d, platformNameMap());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HotSearchDailyAdminVO update(Long id, HotSearchDailyRequest req) {
        HotSearchDaily d = dailyMapper.selectById(id);
        if (d == null) throw new BusinessException(AdminHotSearchErrorCode.DAILY_NOT_FOUND);

        // 唯一约束：排除自身后检查
        Long dup = dailyMapper.selectCount(new LambdaQueryWrapper<HotSearchDaily>()
                .eq(HotSearchDaily::getPlatformCode, req.getPlatformCode())
                .eq(HotSearchDaily::getSnapshotDate, req.getSnapshotDate())
                .eq(HotSearchDaily::getRankNum, req.getRankNum())
                .ne(HotSearchDaily::getId, id));
        if (dup != null && dup > 0) {
            throw new BusinessException(AdminHotSearchErrorCode.DAILY_RANK_DUPLICATED);
        }

        d.setRankNum(req.getRankNum());
        d.setTitle(req.getTitle());
        d.setHotValue(req.getHotValue());
        d.setUrl(req.getUrl());
        d.setSearchCount(req.getSearchCount());
        d.setSnapshotDate(req.getSnapshotDate());
        dailyMapper.updateById(d);
        return toVO(d, platformNameMap());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (dailyMapper.selectById(id) == null) {
            throw new BusinessException(AdminHotSearchErrorCode.DAILY_NOT_FOUND);
        }
        dailyMapper.deleteById(id);
    }

    private Map<String, String> platformNameMap() {
        return platformMapper.selectList(null).stream()
                .collect(Collectors.toMap(HotSearchPlatform::getCode, HotSearchPlatform::getName, (a, b) -> a));
    }

    private HotSearchDailyAdminVO toVO(HotSearchDaily d, Map<String, String> names) {
        HotSearchDailyAdminVO vo = new HotSearchDailyAdminVO();
        vo.setId(d.getId());
        vo.setPlatformCode(d.getPlatformCode());
        vo.setPlatformName(names.getOrDefault(d.getPlatformCode(), d.getPlatformCode()));
        vo.setRankNum(d.getRankNum());
        vo.setTitle(d.getTitle());
        vo.setHotValue(d.getHotValue());
        vo.setUrl(d.getUrl());
        vo.setSearchCount(d.getSearchCount());
        vo.setSnapshotDate(d.getSnapshotDate());
        return vo;
    }
}
