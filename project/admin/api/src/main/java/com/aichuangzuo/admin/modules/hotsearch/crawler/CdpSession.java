package com.aichuangzuo.admin.modules.hotsearch.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单个 CDP 标签页的 WebSocket 会话。
 * 每个 send 同步等响应；Page.loadEventFired 是事件，单独回调入 future。
 */
class CdpSession implements AutoCloseable {

    final CompletableFuture<CdpSession> ready = new CompletableFuture<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final AtomicInteger idGen = new AtomicInteger();
    private final CompletableFuture<Void> loadEvent = new CompletableFuture<>();
    private WebSocket ws;
    private final JsonMapper mapper = JsonMapper.builder().build();

    CdpSession() {
    }

    void attach(WebSocket ws) {
        this.ws = ws;
    }

    WebSocket webSocket() { return ws; }

    JsonNode send(String method, String paramsJson) throws Exception {
        int id = idGen.incrementAndGet();
        String msg = "{\"id\":" + id + ",\"method\":\"" + method + "\""
                + (paramsJson == null || paramsJson.isEmpty() ? "" : ",\"params\":" + paramsJson) + "}";
        CompletableFuture<JsonNode> fut = new CompletableFuture<>();
        pending.put(id, fut);
        // 等待消息真正发送完成再阻塞等响应，否则可能因发送未完成导致响应超时
        ws.sendText(msg, true).get(10, TimeUnit.SECONDS);
        JsonNode resp = fut.get(30, TimeUnit.SECONDS);
        if (resp.has("error")) {
            throw new RuntimeException("CDP " + method + " error: " + resp.get("error").toString());
        }
        return resp.get("result");
    }

    void waitForLoadEvent(long deadlineMs) throws Exception {
        long remaining = deadlineMs - System.currentTimeMillis();
        if (remaining <= 0) remaining = 1000;
        try {
            loadEvent.get(remaining, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // 容忍超时；DOM 也许已经渲染完
        }
    }

    @Override
    public void close() {
        try {
            if (ws != null) ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye").get(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
    }

    /** 创建绑定到本会话的 WebSocket 监听器。 */
    WebSocket.Listener newListener() {
        return new ListenerImpl();
    }

    /** WebSocket 监听器：分派响应到 pending，分派事件到对应 future。 */
    private class ListenerImpl implements WebSocket.Listener {
        private final StringBuilder buf = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
            ready.complete(CdpSession.this);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buf.append(data);
            if (last) {
                String text = buf.toString();
                buf.setLength(0);
                try {
                    JsonNode n = mapper.readTree(text);
                    if (n.has("id")) {
                        int id = n.get("id").asInt();
                        CompletableFuture<JsonNode> fut = pending.remove(id);
                        if (fut != null) fut.complete(n);
                    } else if (n.has("method")) {
                        if ("Page.loadEventFired".equals(n.get("method").asText())) {
                            loadEvent.complete(null);
                        }
                    }
                } catch (Exception ignored) {}
            }
            // 每个 fragment 都要请求下一条，否则分片消息会卡死
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            ready.completeExceptionally(error);
        }
    }
}
