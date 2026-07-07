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

import java.util.ArrayList;
import java.util.List;

/**
 * 走 Chrome DevTools Protocol (CDP) 抓反爬严格 / JS 渲染的平台：微博、B 站、头条。
 * 由 {@link ChromeDevToolsFetcher} 提供 raw HTML，本类只负责解析。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CdpHotSearchFetcher implements HotSearchFetcher {

    private final HotSearchProperties properties;
    private final ChromeDevToolsFetcher cdp;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(HotSearchPlatform platform) {
        String code = platform.getCode();
        return "weibo".equals(code) || "bilibili".equals(code) || "toutiao".equals(code);
    }

    @Override
    public List<HotSearchItem> fetch(HotSearchPlatform platform) {
        return switch (platform.getCode()) {
            case "weibo" -> fetchAndParse("https://s.weibo.com/top/summary", this::parseWeibo);
            case "bilibili" -> fetchAndParse("https://www.bilibili.com/v/popular/rank/all", this::parseBilibili);
            // 头条 /trending/ 是 JS 空壳，热榜数据走 hot-board JSON 接口（导航后落在 <pre> 里）
            case "toutiao" -> fetchAndParse("https://www.toutiao.com/hot-event/hot-board/?origin=toutiao_pc", this::parseToutiao);
            default -> new ArrayList<>();
        };
    }

    private List<HotSearchItem> fetchAndParse(String url, Parser parser) {
        String html = cdp.fetchHtml(url);
        if (html == null || html.isEmpty()) return new ArrayList<>();
        try {
            Document doc = Jsoup.parse(html);
            return parser.parse(doc);
        } catch (Exception e) {
            log.warn("CDP 解析 [{}] 失败: {}", url, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<HotSearchItem> parseWeibo(Document doc) {
        // 微博热搜真实 DOM（s.weibo.com React 渲染后）：class 含 td-02 的 td
        Elements rows = doc.select("td[class*='td-02']");
        List<HotSearchItem> list = new ArrayList<>();
        int rank = 1;
        for (Element row : rows) {
            if (rank > properties.getTopN()) break;
            Element linkEl = row.selectFirst("a");
            if (linkEl == null) continue;
            String title = linkEl.text().trim();
            if (title.isEmpty()) continue;
            HotSearchItem item = new HotSearchItem();
            item.setRank(rank++);
            item.setTitle(title);
            // 热度值在 td-02 的最后一个 span
            Elements spans = row.select("span");
            if (!spans.isEmpty()) {
                item.setHotValue(spans.last().text().trim());
            }
            String href = linkEl.attr("href");
            item.setUrl(href.startsWith("http") ? href : "https://s.weibo.com" + href);
            list.add(item);
        }
        return list;
    }

    private List<HotSearchItem> parseBilibili(Document doc) {
        // B 站真实结构：.rank-list .rank-item
        Elements rows = doc.select(".rank-list .rank-item");
        List<HotSearchItem> list = new ArrayList<>();
        int rank = 1;
        for (Element row : rows) {
            if (rank > properties.getTopN()) break;
            Element titleEl = row.selectFirst(".info a.title");
            Element hotEl = row.selectFirst(".info .detail .data-box");
            if (titleEl == null) continue;
            HotSearchItem item = new HotSearchItem();
            item.setRank(rank++);
            item.setTitle(titleEl.text().trim());
            item.setHotValue(hotEl == null ? null : hotEl.text().trim());
            String href = titleEl.attr("href");
            item.setUrl(href.startsWith("http") ? href : "https:" + href);
            list.add(item);
        }
        return list;
    }

    private List<HotSearchItem> parseToutiao(Document doc) {
        // 头条 hot-board 接口返回 JSON，导航后整体落在 body 文本里（<pre>{...}</pre>）
        List<HotSearchItem> list = new ArrayList<>();
        String text = doc.body() != null ? doc.body().wholeText() : doc.wholeText();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) return list;
        try {
            JsonNode root = objectMapper.readTree(text.substring(start, end + 1));
            JsonNode data = root.path("data");
            if (!data.isArray()) return list;
            int rank = 1;
            for (JsonNode n : data) {
                if (rank > properties.getTopN()) break;
                String title = n.path("Title").asText("").trim();
                if (title.isEmpty()) continue;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(title);
                JsonNode hv = n.path("HotValue");
                item.setHotValue(hv.isMissingNode() || hv.isNull() ? null : hv.asText(null));
                String url = n.path("Url").asText(null);
                item.setUrl(url == null || url.isEmpty() ? null : url);
                list.add(item);
            }
        } catch (Exception e) {
            log.warn("头条 hot-board JSON 解析失败: {}", e.getMessage());
        }
        return list;
    }

    @FunctionalInterface
    private interface Parser {
        List<HotSearchItem> parse(Document doc);
    }
}
