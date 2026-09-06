package com.aichuangzuo.user.modules.membership.service;

import com.aichuangzuo.user.modules.membership.mapper.PlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定价目录缓存 key 版本提供方。
 *
 * 用户端与管理端是两个独立进程，管理端改价后无法直接清用户端的 Caffeine 缓存。
 * 这里以 u_plan / u_benefit / u_plan_benefit 三张表的最大 updated_at 作为缓存 key 的版本：
 * 管理端保存后版本立即变化，下一次请求自然落到新 key 上，旧缓存条目等待 TTL 回收即可。
 */
@Component
@RequiredArgsConstructor
public class PlanCacheVersionKeyProvider {

    private final PlanMapper planMapper;

    /** 返回目录缓存版本字符串；三张表均为空时返回 "0"。 */
    public String version() {
        LocalDateTime ts = planMapper.selectCatalogVersion();
        return ts == null ? "0" : ts.toString();
    }
}
