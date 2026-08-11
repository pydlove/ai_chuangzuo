package com.aichuangzuo.user.modules.lottery.util;

import com.aichuangzuo.user.modules.lottery.mapper.LotteryRedemptionCodeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class LotteryCodeGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int MAX_RETRIES = 5;

    private final LotteryRedemptionCodeMapper redemptionCodeMapper;

    public String generate(String prefix, int length) {
        String safePrefix = prefix == null ? "" : prefix;
        int randomLength = Math.max(1, length - safePrefix.length());
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            StringBuilder sb = new StringBuilder(safePrefix);
            for (int i = 0; i < randomLength; i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            String code = sb.toString();
            if (!exists(code)) {
                return code;
            }
        }
        throw new IllegalStateException("无法生成唯一兑换码，已达到最大重试次数");
    }

    private boolean exists(String code) {
        return redemptionCodeMapper.selectCount(
                new LambdaQueryWrapper<com.aichuangzuo.user.modules.lottery.entity.LotteryRedemptionCode>()
                        .eq(com.aichuangzuo.user.modules.lottery.entity.LotteryRedemptionCode::getCode, code)) > 0;
    }
}
