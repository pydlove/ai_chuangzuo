package com.aichuangzuo.admin.modules.hotsearch.crawler;

import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.properties.HotSearchProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 jsoup 的公开页面抓取器。
 *
 * <p>由于各平台反爬策略和页面结构经常变化，本实现以“尽力抓取”为原则：
 * 单平台失败时返回空列表并记录 warning，不影响其他平台。
 *
 * <p>各平台数据源说明：
 * <ul>
 *   <li>百度：通过 jsoup 抓公开热搜页（依赖 truststore 中含 DLP CA）</li>
 *   <li>抖音：通过 aweme/v1/web/hot/search/list JSON 接口（无需登录）</li>
 *   <li>微博 / B 站 / 头条：需要登录态 cookie（mb、bv、vid等），本实现不可用，
 *       调用 fetch() 时返回空列表并 warn。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsoupHotSearchFetcher implements HotSearchFetcher {

    private final HotSearchProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(HotSearchPlatform platform) {
        String code = platform.getCode();
        return "douyin".equals(code)
                || "toutiao".equals(code)
                || "bilibili".equals(code)
                || "weibo".equals(code)
                || "baidu".equals(code);
    }

    @Override
    public List<HotSearchItem> fetch(HotSearchPlatform platform) {
        return switch (platform.getCode()) {
            case "baidu" -> fetchBaidu();
            case "weibo" -> fetchWeibo();
            case "bilibili" -> fetchBilibili();
            case "toutiao" -> fetchToutiao();
            case "douyin" -> fetchDouyin();
            default -> new ArrayList<>();
        };
    }

    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(properties.getConnectTimeoutMillis())
                .get();
    }

    private List<HotSearchItem> fetchBaidu() {
        try {
            Document doc = fetchDocument("https://top.baidu.com/board?tab=realtime");
            Elements rows = doc.select(".category-wrap_iQLoo");
            List<HotSearchItem> list = new ArrayList<>();
            int rank = 1;
            for (Element row : rows) {
                if (rank > properties.getTopN()) break;
                Element titleEl = row.selectFirst(".c-single-text-ellipsis");
                Element hotEl = row.selectFirst(".hot-index_1Bl1a");
                if (titleEl == null) continue;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(titleEl.text().trim());
                item.setHotValue(hotEl == null ? null : hotEl.text().trim());
                item.setUrl(null);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.warn("百度热搜抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<HotSearchItem> fetchToutiao() {
        // 头条公开热搜页 URL 已弃用，热搜数据需登录态。无登录可用数据源，跳过。
        log.warn("今日头条暂不支持：需要登录态 cookie 才能拿到热搜");
        return new ArrayList<>();
    }

    private List<HotSearchItem> fetchDouyin() {
        // 抖音热搜走 aweme/v1/web/hot/search/list JSON 接口，无登录可用
        try {
            String url = "https://www.douyin.com/aweme/v1/web/hot/search/list/"
                    + "?device_platform=webapp&aid=6383&channel=channel_pc_web"
                    + "&version_code=190500&version_name=19.5.0"
                    + "&cookie_enabled=true&platform=PC";
            String body = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                    .header("Referer", "https://www.douyin.com/")
                    .ignoreContentType(true)
                    .timeout(properties.getReadTimeoutMillis())
                    .get()
                    .body()
                    .text();
            JsonNode root = objectMapper.readTree(body);
            JsonNode wordList = root.path("data").path("word_list");
            if (!wordList.isArray()) {
                log.warn("抖音热搜响应格式异常：无 word_list");
                return new ArrayList<>();
            }
            List<HotSearchItem> list = new ArrayList<>();
            int rank = 1;
            for (JsonNode w : wordList) {
                if (rank > properties.getTopN()) break;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(w.path("word").asText(""));
                JsonNode hv = w.path("hot_value");
                item.setHotValue(hv.isNumber() ? String.valueOf(hv.asLong()) : hv.asText(null));
                item.setUrl(null);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.warn("抖音热搜抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<HotSearchItem> fetchBilibili() {
        // B 站 ranking API 需要登录态 SESSDATA，否则 -352 风控。无登录可用数据源，跳过。
        log.warn("B 站暂不支持：ranking API 需要登录 SESSDATA cookie 才能拿到数据");
        return new ArrayList<>();
    }

    private List<HotSearchItem> fetchWeibo() {
        // 微博 s.weibo.com/top/summary 走 Sina Visitor System 反爬墙，HTML 是空 JS 挑战页。
        // m.weibo.cn 移动 API 也走同一套。无登录可用数据源，跳过。
        log.warn("微博暂不支持：Sina Visitor System 反爬墙阻挡，需要 JS 引擎或登录 cookie");
        return new ArrayList<>();
    }
}
