package com.aichuangzuo.shared.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link ModelConfigSelectorAutoConfig} 通过 Spring Boot 自动装配机制
 * 在 admin / user 两端都能拿到 {@link ModelConfigSelector} bean。
 */
class ModelConfigSelectorAutoConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ModelConfigSelectorAutoConfig.class));

    @Test
    void autoConfig_registersSingleSelectorBean() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(ModelConfigSelector.class));
    }
}
