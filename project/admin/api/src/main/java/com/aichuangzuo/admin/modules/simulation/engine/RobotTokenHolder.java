package com.aichuangzuo.admin.modules.simulation.engine;

import com.aichuangzuo.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 模拟运营-机器人 token 缓存：内存缓存，401 时重新登录并重试一次。
 */
@Slf4j
@Component
public class RobotTokenHolder {

    private final Map<Long, String> cache = new ConcurrentHashMap<>();

    /** 取缓存 token；无则登录。 */
    public String token(RobotContext ctx) {
        String cached = cache.get(ctx.robot.getId());
        if (cached != null) {
            return cached;
        }
        return relogin(ctx);
    }

    public void invalidate(Long robotId) {
        cache.remove(robotId);
    }

    /** 用 token 执行一段调用；遇 401（机器人 token 过期）重新登录后重试一次。 */
    public void execute(RobotContext ctx, Consumer<String> action) {
        try {
            action.accept(token(ctx));
        } catch (BusinessException e) {
            if (e.getCode() != null && e.getCode() == 401) {
                log.info("模拟机器人 token 过期，重新登录 robotId={}", ctx.robot.getId());
                action.accept(relogin(ctx));
            } else {
                throw e;
            }
        }
    }

    private String relogin(RobotContext ctx) {
        String token = ctx.userApi.login(ctx.robot.getEmail(), ctx.plainPassword);
        cache.put(ctx.robot.getId(), token);
        return token;
    }
}
