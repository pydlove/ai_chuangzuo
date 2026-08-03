package com.aichuangzuo.admin.modules.hotsearch.service.impl;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchCrawlLogQueryRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchCrawlLog;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchCrawlLogMapper;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchCrawlLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HotSearchCrawlLogServiceImpl implements HotSearchCrawlLogService {

    private final HotSearchCrawlLogMapper crawlLogMapper;

    @Override
    public PageResult list(HotSearchCrawlLogQueryRequest request) {
        LambdaQueryWrapper<HotSearchCrawlLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getTriggerType())) {
            wrapper.eq(HotSearchCrawlLog::getTriggerType, request.getTriggerType());
        }
        wrapper.orderByDesc(HotSearchCrawlLog::getStartedAt);
        Page<HotSearchCrawlLog> page = new Page<>(request.getPage(), request.getSize());
        Page<HotSearchCrawlLog> result = crawlLogMapper.selectPage(page, wrapper);
        return new PageResult(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveLog(HotSearchCrawlLog log) {
        crawlLogMapper.insert(log);
    }
}
