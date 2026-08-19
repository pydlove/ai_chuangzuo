package com.aichuangzuo.user.modules.security.accesscontrol.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessControlSnapshot {

    private Set<String> blackIps = Collections.emptySet();
    private Set<String> whiteIps = Collections.emptySet();
    private Set<String> blackAccounts = Collections.emptySet();
    private Set<String> whiteAccounts = Collections.emptySet();

    public boolean isIpAllowed(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        if (whiteIps != null && !whiteIps.isEmpty()) {
            return whiteIps.contains(ip);
        }
        return blackIps == null || !blackIps.contains(ip);
    }

    public boolean isAccountAllowed(String account) {
        if (account == null || account.isBlank()) {
            return true;
        }
        if (whiteAccounts != null && !whiteAccounts.isEmpty()) {
            return whiteAccounts.contains(account);
        }
        return blackAccounts == null || !blackAccounts.contains(account);
    }
}
