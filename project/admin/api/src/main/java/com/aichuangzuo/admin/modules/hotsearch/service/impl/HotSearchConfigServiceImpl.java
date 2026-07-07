package com.aichuangzuo.admin.modules.hotsearch.service.impl;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchConfigRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchConfig;
import com.aichuangzuo.admin.modules.hotsearch.enums.AdminHotSearchErrorCode;
import com.aichuangzuo.admin.modules.hotsearch.job.HotSearchCrawlJob;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchConfigMapper;
import com.aichuangzuo.admin.modules.hotsearch.properties.HotSearchProperties;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchConfigService;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotSearchConfigServiceImpl implements HotSearchConfigService {

    private static final long CONFIG_ID = 1L;

    private final HotSearchConfigMapper configMapper;
    private final HotSearchProperties properties;
    private final HotSearchCrawlJob crawlJob;

    @Override
    public HotSearchConfig getConfig() {
        HotSearchConfig cfg = configMapper.selectById(CONFIG_ID);
        if (cfg == null) {
            throw new BusinessException(AdminHotSearchErrorCode.CONFIG_NOT_FOUND);
        }
        return cfg;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HotSearchConfig saveConfig(HotSearchConfigRequest req, Long updatedBy) {
        try {
            new CronTrigger(req.getCron());
        } catch (Exception e) {
            throw new BusinessException(AdminHotSearchErrorCode.INVALID_CRON);
        }

        HotSearchConfig existing = configMapper.selectById(CONFIG_ID);
        HotSearchConfig entity = existing == null ? new HotSearchConfig() : existing;
        entity.setId(CONFIG_ID);
        entity.setCron(req.getCron());
        entity.setEnabled(req.getEnabled());
        entity.setTopN(req.getTopN());
        entity.setConnectTimeoutMillis(req.getConnectTimeoutMillis());
        entity.setReadTimeoutMillis(req.getReadTimeoutMillis());
        entity.setUpdatedBy(updatedBy == null ? 0L : updatedBy);

        if (existing == null) {
            entity.setCreatedAt(LocalDateTime.now());
            configMapper.insert(entity);
        } else {
            configMapper.updateById(entity);
        }

        // 重建定时 Trigger
        crawlJob.reschedule();
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromProperties() {
        HotSearchConfig existing = configMapper.selectById(CONFIG_ID);
        if (existing != null) {
            return;
        }
        HotSearchConfig entity = new HotSearchConfig();
        entity.setId(CONFIG_ID);
        entity.setCron(properties.getCron() != null ? properties.getCron() : "0 0 2 * * ?");
        entity.setEnabled(properties.isCrawlEnabled() ? 1 : 0);
        entity.setTopN(properties.getTopN() != null ? properties.getTopN() : 50);
        entity.setConnectTimeoutMillis(properties.getConnectTimeoutMillis() != null ? properties.getConnectTimeoutMillis() : 5000);
        entity.setReadTimeoutMillis(properties.getReadTimeoutMillis() != null ? properties.getReadTimeoutMillis() : 10000);
        entity.setUpdatedBy(0L);
        configMapper.insert(entity);
        log.info("热搜配置默认值已写入 DB");
    }
}
