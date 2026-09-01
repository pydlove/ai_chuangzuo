package com.aichuangzuo.admin.modules.experience.util;

import com.aichuangzuo.admin.modules.experience.mapper.ExperienceTokenMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 体验令牌生成器。
 */
@Component
@RequiredArgsConstructor
public class ExperienceTokenGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String PREFIX = "EXP_";
    private static final int RANDOM_LENGTH = 8;
    private static final int MAX_RETRIES = 5;
    private static final DateTimeFormatter BATCH_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ExperienceTokenMapper experienceTokenMapper;

    public String generateBatchId() {
        return "B" + LocalDateTime.now().format(BATCH_FMT);
    }

    public String generateToken() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            StringBuilder sb = new StringBuilder(PREFIX);
            for (int i = 0; i < RANDOM_LENGTH; i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            String token = sb.toString();
            if (!exists(token)) {
                return token;
            }
        }
        throw new IllegalStateException("无法生成唯一体验令牌，已达到最大重试次数");
    }

    private boolean exists(String token) {
        return experienceTokenMapper.selectCount(
                new LambdaQueryWrapper<com.aichuangzuo.shared.entity.ExperienceToken>()
                        .eq(com.aichuangzuo.shared.entity.ExperienceToken::getToken, token)) > 0;
    }
}
