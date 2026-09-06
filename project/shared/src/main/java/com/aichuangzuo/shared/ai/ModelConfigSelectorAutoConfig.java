package com.aichuangzuo.shared.ai;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 共享模型资源池选择器的自动装配：admin / user 两端引入 shared 模块后自动获得
 * {@link ModelConfigSelector} bean，无需在两端各自的 {@code @SpringBootApplication} 上手动扫描。
 */
@AutoConfiguration
public class ModelConfigSelectorAutoConfig {

    @Bean
    public ModelConfigSelector modelConfigSelector() {
        return new ModelConfigSelector();
    }
}
