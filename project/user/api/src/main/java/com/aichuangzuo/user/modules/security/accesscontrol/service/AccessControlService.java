package com.aichuangzuo.user.modules.security.accesscontrol.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.security.accesscontrol.entity.AccessControl;
import com.aichuangzuo.user.modules.security.accesscontrol.mapper.AccessControlMapper;
import com.aichuangzuo.user.modules.security.accesscontrol.vo.AccessControlSnapshot;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final AccessControlMapper accessControlMapper;
    private final UserMapper userMapper;

    @Cacheable(cacheNames = "accessControl", key = "'snapshot'")
    public AccessControlSnapshot loadActiveRules() {
        List<AccessControl> list = accessControlMapper.selectList(
                Wrappers.<AccessControl>lambdaQuery()
                        .eq(AccessControl::getIsDeleted, 0)
                        .eq(AccessControl::getRuleStatus, 1));

        Set<String> blackIps = new HashSet<>();
        Set<String> whiteIps = new HashSet<>();
        Set<String> blackAccounts = new HashSet<>();
        Set<String> whiteAccounts = new HashSet<>();

        for (AccessControl rule : list) {
            if (rule.getRuleType() == null || rule.getListType() == null || rule.getRuleValue() == null) {
                continue;
            }
            String value = rule.getRuleValue().trim();
            if (value.isEmpty()) {
                continue;
            }
            if (rule.getRuleType() == 1) {
                if (rule.getListType() == 1) {
                    blackIps.add(value);
                } else if (rule.getListType() == 2) {
                    whiteIps.add(value);
                }
            } else if (rule.getRuleType() == 2) {
                if (rule.getListType() == 1) {
                    blackAccounts.add(value);
                } else if (rule.getListType() == 2) {
                    whiteAccounts.add(value);
                }
            }
        }

        return new AccessControlSnapshot(blackIps, whiteIps, blackAccounts, whiteAccounts);
    }

    @Cacheable(cacheNames = "accessControlUser", key = "#userId")
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
}
