package com.aichuangzuo.admin.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinimaxProviderClientTest {

    private final MinimaxProviderClient client = new MinimaxProviderClient();

    @Test
    void trim_shouldRemoveTrailingV1() {
        assertEquals("https://api.minimax.chat",
                invokeTrim("https://api.minimax.chat/v1"));
        assertEquals("https://api.minimax.chat",
                invokeTrim("https://api.minimax.chat/v1/"));
    }

    @Test
    void trim_shouldPreserveProxyPath() {
        assertEquals("https://api.example.com/minimax",
                invokeTrim("https://api.example.com/minimax/"));
        assertEquals("https://api.example.com/minimax",
                invokeTrim("https://api.example.com/minimax"));
        assertEquals("https://api.example.com/minimax",
                invokeTrim("https://api.example.com/minimax/v1"));
        assertEquals("https://api.example.com/minimax",
                invokeTrim("https://api.example.com/minimax/v1/"));
    }

    @Test
    void trim_shouldHandleCleanBaseUrl() {
        assertEquals("https://api.minimax.chat",
                invokeTrim("https://api.minimax.chat"));
        assertEquals("https://api.minimax.chat",
                invokeTrim("https://api.minimax.chat/"));
    }

    @Test
    void trim_shouldHandleNull() {
        assertEquals("", invokeTrim(null));
    }

    private String invokeTrim(String baseUrl) {
        return (String) ReflectionTestUtils.invokeMethod(client, "trim", baseUrl);
    }
}
