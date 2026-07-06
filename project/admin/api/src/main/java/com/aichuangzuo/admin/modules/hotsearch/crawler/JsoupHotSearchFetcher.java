package com.aichuangzuo.admin.modules.hotsearch.crawler;

import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.properties.HotSearchProperties;
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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsoupHotSearchFetcher implements HotSearchFetcher {

    private final HotSearchProperties properties;

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

    private List<HotSearchItem> fetchWeibo() {
        try {
            Document doc = fetchDocument("https://s.weibo.com/top/summary");
            Elements rows = doc.select("#pl_top_realtimehot tbody tr");
            List<HotSearchItem> list = new ArrayList<>();
            int rank = 1;
            for (Element row : rows) {
                if (rank > properties.getTopN()) break;
                Element ranktop = row.selectFirst("td.ranktop");
                if (ranktop != null) {
                    continue;
                }
                Element linkEl = row.selectFirst("td.td-02 a");
                Element hotEl = row.selectFirst("td.td-02 span");
                if (linkEl == null) continue;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(linkEl.text().trim());
                item.setHotValue(hotEl == null ? null : hotEl.text().trim());
                String href = linkEl.attr("href");
                item.setUrl(href.startsWith("http") ? href : "https://s.weibo.com" + href);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.warn("微博热搜抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<HotSearchItem> fetchBilibili() {
        try {
            Document doc = fetchDocument("https://www.bilibili.com/v/popular/rank/all");
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
        } catch (Exception e) {
            log.warn("B 站热门抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<HotSearchItem> fetchToutiao() {
        try {
            Document doc = fetchDocument("https://www.toutiao.com/hot-event/hot-board/?origin=toutiao_pc");
            Elements rows = doc.select("[class*='hot-board']");
            List<HotSearchItem> list = new ArrayList<>();
            int rank = 1;
            for (Element row : rows) {
                if (rank > Math.min(properties.getTopN(), 5)) break;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(row.text().trim());
                item.setHotValue(null);
                item.setUrl(null);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.warn("今日头条热搜抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<HotSearchItem> fetchDouyin() {
        try {
            String keyword = URLEncoder.encode("热点", StandardCharsets.UTF_8);
            Document doc = fetchDocument("https://www.douyin.com/search/" + keyword);
            Elements rows = doc.select("[data-e2e='search-card-title'], .search-card-title");
            List<HotSearchItem> list = new ArrayList<>();
            int rank = 1;
            for (Element row : rows) {
                if (rank > Math.min(properties.getTopN(), 5)) break;
                HotSearchItem item = new HotSearchItem();
                item.setRank(rank++);
                item.setTitle(row.text().trim());
                item.setHotValue(null);
                item.setUrl(null);
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.warn("抖音热搜抓取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
