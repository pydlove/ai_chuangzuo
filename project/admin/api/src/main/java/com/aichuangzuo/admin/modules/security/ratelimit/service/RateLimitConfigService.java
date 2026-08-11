package com.aichuangzuo.admin.modules.security.ratelimit.service;

import com.aichuangzuo.admin.modules.security.ratelimit.dto.request.RateLimitConfigUpdateRequest;
import com.aichuangzuo.admin.modules.security.ratelimit.entity.RateLimitConfig;
import com.aichuangzuo.admin.modules.security.ratelimit.mapper.RateLimitConfigMapper;
import com.aichuangzuo.admin.modules.security.ratelimit.vo.RateLimitConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录限流配置服务。
 *
 * <p>单行配置（id=1），admin 端 GET/PUT 维护。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitConfigService {

    private static final long CONFIG_ID = 1L;
    private static final int DEFAULT_ENABLED = 1;

    private final RateLimitConfigMapper mapper;

    public RateLimitConfigVO detail() {
        RateLimitConfig config = mapper.selectById(CONFIG_ID);
        if (config == null) {
            config = new RateLimitConfig();
            config.setId(CONFIG_ID);
            config.setIsLoginRateLimitEnabled(DEFAULT_ENABLED);
        }
        return toVo(config);
    }

    @Transactional
    public RateLimitConfigVO update(RateLimitConfigUpdateRequest req, Long adminUserId) {
        RateLimitConfig exist = mapper.selectById(CONFIG_ID);
        boolean isNew = exist == null;
        if (isNew) {
            exist = new RateLimitConfig();
            exist.setId(CONFIG_ID);
        }
        exist.setIsLoginRateLimitEnabled(req.getIsLoginRateLimitEnabled());
        exist.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        if (isNew) {
            mapper.insert(exist);
        } else {
            mapper.updateById(exist);
        }

        log.info("admin={} 更新登录限流配置 isLoginRateLimitEnabled={}", adminUserId, exist.getIsLoginRateLimitEnabled());
        return toVo(exist);
    }

    private RateLimitConfigVO toVo(RateLimitConfig c) {
        RateLimitConfigVO vo = new RateLimitConfigVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
