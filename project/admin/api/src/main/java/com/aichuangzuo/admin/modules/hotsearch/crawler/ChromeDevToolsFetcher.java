package com.aichuangzuo.admin.modules.hotsearch.crawler;

import com.aichuangzuo.admin.modules.hotsearch.properties.HotSearchProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于 Chrome DevTools Protocol (CDP) 的 headless 浏览器抓取器。
 *
 * <p>启动本地 Chrome（系统已装或自配），通过 JSON-RPC over WebSocket 驱动：
 * 创建标签页 → 导航 → 等待 loadEventFired → 读 DOM outerHTML → 关闭标签页。
 *
 * <p>优势：复用系统 Chrome，无需下载 Chromium；支持 JS 渲染 + 复杂反爬。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChromeDevToolsFetcher {

    private final HotSearchProperties properties;

    @Value("${hot-search.chrome-path:}")
    private String chromePath;

    @Value("${hot-search.chrome-debugging-port:9222}")
    private int debuggingPort;

    private Process chromeProcess;
    private Path userDataDir;
    private final HttpClient http = HttpClient.newHttpClient();
    private final AtomicInteger msgId = new AtomicInteger();

    @PostConstruct
    public synchronized void start() {
        try {
            doStart();
        } catch (Exception e) {
            log.error("CDP Chrome 启动失败，相关平台（微博/B站/头条）将不可用：{}", e.getMessage(), e);
            chromeProcess = null;
        }
    }

    /**
     * 运行期发现 Chrome 进程死亡时调用：清理旧进程并重新启动。
     */
    public synchronized void restart() throws IOException {
        doStop();
        doStart();
    }

    @PreDestroy
    public synchronized void stop() {
        doStop();
    }

    private void doStart() throws IOException {
        if (userDataDir != null) {
            doStop();
        }
        String resolvedChromePath = resolveChromePath();
        userDataDir = Files.createTempDirectory("hotsearch-cdp-");
        List<String> cmd = new ArrayList<>();
        cmd.add(resolvedChromePath);
        cmd.add("--headless=new");
        cmd.add("--no-sandbox");
        cmd.add("--disable-setuid-sandbox");
        cmd.add("--disable-gpu");
        cmd.add("--disable-software-rasterizer");
        cmd.add("--disable-dev-shm-usage");
        cmd.add("--disable-background-networking");
        cmd.add("--user-data-dir=" + userDataDir.toString());
        cmd.add("--remote-debugging-port=" + debuggingPort);
        cmd.add("--remote-allow-origins=*");
        cmd.add("about:blank");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Path stderrFile = Files.createTempFile("hotsearch-cdp-stderr-", ".log");
        pb.redirectError(stderrFile.toFile());
        chromeProcess = pb.start();
        // 等 Chrome 启动并监听端口
        if (!waitForPort(debuggingPort, 20)) {
            int exitCode = chromeProcess.isAlive() ? -1 : chromeProcess.exitValue();
            String stderr = readTail(stderrFile, 4096);
            doStop();
            Files.deleteIfExists(stderrFile);
            String hint = exitCode == 127
                    ? "（退出码 127 通常表示命令未找到或缺少动态链接库，可执行 ldd " + resolvedChromePath + " 检查）"
                    : "（通常是缺少 headless 依赖库）";
            throw new IOException("Chrome 启动后未监听 " + debuggingPort + "，进程退出码=" + exitCode + " " + hint
                    + "。Chrome stderr: " + stderr);
        }
        Files.deleteIfExists(stderrFile);
        log.info("CDP Chrome 已启动，path={} port={} pid={}", resolvedChromePath, debuggingPort, chromeProcess.pid());
    }

    private void doStop() {
        if (chromeProcess != null && chromeProcess.isAlive()) {
            chromeProcess.destroyForcibly();
        }
        chromeProcess = null;
        if (userDataDir != null) {
            Path dir = userDataDir;
            userDataDir = null;
            try {
                Files.walk(dir)
                        .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            } catch (IOException ignored) {}
        }
    }

    private String resolveChromePath() throws IOException {
        if (StringUtils.hasText(chromePath)) {
            Path configured = Path.of(chromePath);
            if (Files.exists(configured) && !Files.isDirectory(configured)) {
                return configured.toAbsolutePath().toString();
            }
            log.warn("配置的 Chrome 路径不存在或不是可执行文件：{}，尝试自动探测常见路径", chromePath);
        }

        String[] candidates = {
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                "/usr/bin/google-chrome",
                "/usr/bin/chromium",
                "/usr/bin/chromium-browser",
                "/snap/bin/chromium",
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
        };
        for (String candidate : candidates) {
            Path path = Path.of(candidate);
            if (Files.exists(path) && !Files.isDirectory(path)) {
                log.info("自动探测到 Chrome/Chromium：{}", candidate);
                return path.toAbsolutePath().toString();
            }
        }

        throw new IOException(
                "未找到 Chrome/Chromium 可执行文件。请在服务器上安装后，通过配置 hot-search.chrome-path 指定路径（例如 /usr/bin/chromium），"
                        + "或通过环境变量 HOT_SEARCH_CHROME_PATH 覆盖。"
        );
    }

    private static String readTail(Path file, int maxBytes) {
        try {
            long size = Files.size(file);
            if (size == 0) {
                return "";
            }
            long skip = Math.max(0, size - maxBytes);
            try (var in = java.nio.file.Files.newInputStream(file)) {
                in.skip(skip);
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (IOException e) {
            return "";
        }
    }

    private synchronized boolean ensureChromeRunning() {
        if (chromeProcess != null && chromeProcess.isAlive()) {
            return true;
        }
        log.warn("CDP Chrome 未运行，尝试重新启动");
        try {
            restart();
            if (chromeProcess != null && chromeProcess.isAlive()) {
                log.info("CDP Chrome 重新启动成功，pid={}", chromeProcess.pid());
                return true;
            }
        } catch (Exception e) {
            log.error("CDP Chrome 重新启动失败: {}", e.getMessage(), e);
        }
        return false;
    }

    private boolean waitForPort(int port, int maxSeconds) {
        for (int i = 0; i < maxSeconds; i++) {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/json/version"))
                        .timeout(Duration.ofSeconds(1))
                        .GET().build();
                HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 200) return true;
            } catch (Exception ignored) {}
            try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return false; }
        }
        return false;
    }

    /**
     * 用 Chrome 打开 URL，等待 load + 短暂渲染后，读取 outerHTML。
     */
    public String fetchHtml(String url) {
        if (!ensureChromeRunning()) {
            log.warn("Chrome 进程未运行，跳过 {}", url);
            return "";
        }
        // 1) 创建空白标签页（先建标签页再连 WebSocket，最后显式导航，
        //    否则 loadEventFired 会在订阅前触发导致读到空 DOM）
        String targetId = createTarget();
        if (targetId == null) {
            log.warn("Chrome 创建标签页失败：{}", url);
            return "";
        }
        try {
            // 2) 找到这个标签页的 WebSocket URL
            String wsUrl = findWebSocketUrl(targetId);
            if (wsUrl == null) {
                log.warn("Chrome 找不到标签页 {} 的 WebSocket URL", targetId);
                return "";
            }
            // 3) 用 WebSocket 驱动 CDP：先订阅事件，再导航
            try (CdpSession session = openSession(wsUrl)) {
                session.send("Page.enable", null);
                session.send("Page.navigate", "{\"url\":\"" + escapeJson(url) + "\"}");
                long deadline = System.currentTimeMillis() + properties.getReadTimeoutMillis();
                session.waitForLoadEvent(deadline);
                // 给 JS 额外 2s 渲染
                Thread.sleep(2000);
                // Runtime.evaluate 比 DOM.getDocument 更稳定，避免大 DOM 序列化分片问题
                com.fasterxml.jackson.databind.JsonNode result = session.send("Runtime.evaluate",
                        "{\"expression\":\"document.documentElement.outerHTML\",\"returnByValue\":true}");
                if (result == null) return "";
                String html = result.path("result").path("value").asText("");
                return html;
            }
        } catch (Exception e) {
            log.warn("CDP 抓取 [{}] 失败: {}", url, e.getMessage(), e);
            return "";
        } finally {
            closeTarget(targetId);
        }
    }

    private String createTarget() {
        try {
            String body = "{\"url\":\"about:blank\"}";
            HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort + "/json/new"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) return null;
            com.fasterxml.jackson.databind.JsonNode root = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(r.body());
            return root.path("id").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void closeTarget(String targetId) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort + "/json/close/" + targetId))
                    .timeout(Duration.ofSeconds(3))
                    .GET().build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {}
    }

    private String findWebSocketUrl(String targetId) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + debuggingPort + "/json"))
                    .timeout(Duration.ofSeconds(3))
                    .GET().build();
            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            com.fasterxml.jackson.databind.JsonNode arr = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(r.body());
            for (com.fasterxml.jackson.databind.JsonNode t : arr) {
                if (targetId.equals(t.path("id").asText())) {
                    return t.path("webSocketDebuggerUrl").asText(null);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private CdpSession openSession(String wsUrl) throws Exception {
        CdpSession session = new CdpSession();
        WebSocket.Listener listener = session.newListener();
        WebSocket ws = http.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(URI.create(wsUrl), listener)
                .get(10, TimeUnit.SECONDS);
        session.attach(ws);
        session.ready.get(5, TimeUnit.SECONDS);
        return session;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
