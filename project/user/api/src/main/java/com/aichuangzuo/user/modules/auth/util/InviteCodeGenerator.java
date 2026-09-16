package com.aichuangzuo.user.modules.auth.util;

import com.aichuangzuo.shared.exception.SystemException;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 用户邀请码生成器：6 位去易混字符，查重 10 次。
 */
@Component
@RequiredArgsConstructor
public class InviteCodeGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final UserMapper userMapper;

    public String generate() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            String code = sb.toString();
            if (userMapper.selectByInviteCode(code) == null) {
                return code;
            }
        }
        throw new SystemException("生成邀请码失败");
    }
}
