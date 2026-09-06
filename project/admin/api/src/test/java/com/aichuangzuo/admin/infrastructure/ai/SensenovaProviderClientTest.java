package com.aichuangzuo.admin.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensenovaProviderClientTest {

    private final SensenovaProviderClient client = new SensenovaProviderClient();

    @Test
    void apiPrefix_shouldBeV1() {
        assertEquals("/v1", ReflectionTestUtils.invokeMethod(client, "apiPrefix"));
    }

    @Test
    void trim_shouldRemoveTrailingVersionSegment() {
        assertEquals("https://token.sensenova.cn",
                invokeTrim("https://token.sensenova.cn/v1"));
        assertEquals("https://token.sensenova.cn",
                invokeTrim("https://token.sensenova.cn/v1/"));
    }

    @Test
    void trim_shouldPreserveProxyPath() {
        assertEquals("https://api.example.com/sensenova",
                invokeTrim("https://api.example.com/sensenova/v1"));
    }

    @Test
    void trim_shouldHandleCleanBaseUrl() {
        assertEquals("https://token.sensenova.cn",
                invokeTrim("https://token.sensenova.cn"));
    }

    @Test
    void trim_shouldHandleNull() {
        assertEquals("", invokeTrim(null));
    }

    private String invokeTrim(String baseUrl) {
        return (String) ReflectionTestUtils.invokeMethod(client, "trim", baseUrl);
    }
}
