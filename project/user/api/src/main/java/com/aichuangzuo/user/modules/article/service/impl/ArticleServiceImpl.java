package com.aichuangzuo.user.modules.article.service.impl;

import com.aichuangzuo.user.infrastructure.security.JwtUtil;
import com.aichuangzuo.user.modules.article.dto.request.SaveArticleRequest;
import com.aichuangzuo.user.modules.article.dto.request.UpdateArticleRequest;
import com.aichuangzuo.user.modules.article.entity.Article;
import com.aichuangzuo.shared.enums.error.ArticleErrorCode;
import com.aichuangzuo.user.modules.article.mapper.ArticleMapper;
import com.aichuangzuo.user.modules.article.service.ArticleService;
import com.aichuangzuo.user.modules.article.vo.ArticlePageVO;
import com.aichuangzuo.user.modules.article.vo.ArticleVO;
import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aichuangzuo.shared.enums.DeletedFlagEnum;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户作品服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private static final String DEFAULT_STYLE_OVERRIDES_JSON = "{\"blocks\":{},\"inlines\":[]}";

    private final ArticleMapper articleMapper;
    private final ObjectMapper objectMapper;
    private final SkillMarketMapper skillMarketMapper;
    private final UserSkillMapper userSkillMapper;
    private final JwtUtil jwtUtil;

    private static final Pattern MARKDOWN_HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    @Override
    public ArticlePageVO list(Long userId, String keyword, long page, long pageSize) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getUserId, userId)
                .eq(Article::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode())
                .orderByDesc(Article::getCompletedAt)
                .orderByDesc(Article::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Article::getTitle, keyword.trim());
        }
        IPage<Article> result = articleMapper.selectPage(new Page<>(page, pageSize), wrapper);
        ArticlePageVO vo = new ArticlePageVO();
        vo.setList(result.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        vo.setTotal(result.getTotal());
        vo.setPage(result.getCurrent());
        vo.setPageSize(result.getSize());
        return vo;
    }

    @Override
    public ArticleVO get(Long userId, String bizNo) {
        Article article = mustFind(userId, bizNo);
        return toVo(article);
    }

    @Override
    public ArticleVO getByTaskId(Long userId, Long taskId) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getTaskId, taskId)
                .eq(Article::getUserId, userId)
                .eq(Article::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode())
                .last("LIMIT 1"));
        if (article == null) {
            throw new BusinessException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }
        return toVo(article);
    }

    @Override
    public ArticleVO getInternal(String bizNo) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getBizNo, bizNo)
                .eq(Article::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode()));
        if (article == null) {
            throw new BusinessException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }
        return toVo(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String save(Long userId, SaveArticleRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(ArticleErrorCode.ARTICLE_TITLE_EMPTY);
        }
        if (!StringUtils.hasText(request.getBody())) {
            throw new BusinessException(ArticleErrorCode.ARTICLE_BODY_EMPTY);
        }
        if (request.getTaskId() != null) {
            Article existing = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                    .eq(Article::getTaskId, request.getTaskId())
                    .eq(Article::getUserId, userId)
                    .eq(Article::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode())
                    .last("LIMIT 1"));
            if (existing != null) {
                log.info("taskId={} 已存在作品，直接返回已有 bizNo={}", request.getTaskId(), existing.getBizNo());
                return existing.getBizNo();
            }
        }

        Article article = new Article();
        article.setBizNo(generateBizNo());
        article.setUserId(userId);
        article.setTaskId(request.getTaskId());
        article.setTitle(request.getTitle().trim());
        article.setBody(request.getBody());
        article.setStyleOverrides(normalizeStyleOverrides(request.getStyleOverrides()));
        article.setPlatform(request.getPlatform());
        article.setSkill(request.getSkill());
        article.setTemplate(request.getTemplate());
        article.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        article.setTagsJson(toTagsJson(request.getTags()));
        article.setAiDetectReport(toAiDetectReportJson(request.getAiDetectReport()));
        article.setWordCount(request.getWordCount() == null ? 0 : Math.max(0, request.getWordCount()));
        article.setCompletedAt(request.getCompletedAt() != null ? request.getCompletedAt() : LocalDateTime.now());
        articleMapper.insert(article);
        log.info("保存作品完成 userId={}, bizNo={}, title={}", userId, article.getBizNo(), article.getTitle());
        return article.getBizNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, String bizNo, UpdateArticleRequest request) {
        mustFind(userId, bizNo);
        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Article::getUserId, userId)
                .eq(Article::getBizNo, bizNo)
                .eq(Article::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode());
        boolean touched = false;
        if (StringUtils.hasText(request.getTitle())) {
            wrapper.set(Article::getTitle, request.getTitle().trim());
            touched = true;
        }
        if (StringUtils.hasText(request.getBody())) {
            wrapper.set(Article::getBody, request.getBody());
            touched = true;
        }
        if (request.getStyleOverrides() != null) {
            wrapper.set(Article::getStyleOverrides, normalizeStyleOverrides(request.getStyleOverrides()));
            touched = true;
        }
        if (!touched) {
            return;
        }
        articleMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, String bizNo) {
        mustFind(userId, bizNo);
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getUserId, userId)
                .eq(Article::getBizNo, bizNo)
                .eq(Article::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode())
                .set(Article::getIsDeleted, DeletedFlagEnum.DELETED.getCode()));
    }

    private Article mustFind(Long userId, String bizNo) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getUserId, userId)
                .eq(Article::getBizNo, bizNo)
                .eq(Article::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode()));
        if (article == null) {
            throw new BusinessException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }
        return article;
    }

    private ArticleVO toVo(Article article) {
        ArticleVO vo = new ArticleVO();
        vo.setBizNo(article.getBizNo());
        vo.setTitle(article.getTitle());
        vo.setBody(article.getBody());
        vo.setStyleOverrides(parseStyleOverrides(article.getStyleOverrides()));
        vo.setPlatform(article.getPlatform());
        vo.setSkill(article.getSkill());
        vo.setSkillName(resolveSkillName(article.getSkill()));
        vo.setTemplate(article.getTemplate());
        vo.setDescription(article.getDescription());
        vo.setTags(parseTags(article.getTagsJson()));
        vo.setAiDetectReport(parseAiDetectReport(article.getAiDetectReport()));
        vo.setWordCount(article.getWordCount());
        vo.setCompletedAt(article.getCompletedAt());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        return vo;
    }

    /**
     * 解析风格可读名称。
     * <p>优先按 {@code skill}（bizNo）查 {@code u_skill_market} 和 {@code u_user_skill}，
     * 命中则返回对应中文名称；未命中则回显 skill 字段本身（兼容旧数据/直接保存名称的场景）。
     */
    private String resolveSkillName(String skill) {
        if (!StringUtils.hasText(skill)) {
            return null;
        }
        SkillMarket market = skillMarketMapper.selectOne(
                new LambdaQueryWrapper<SkillMarket>()
                        .eq(SkillMarket::getBizNo, skill)
                        .eq(SkillMarket::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode())
                        .last("LIMIT 1"));
        if (market != null && StringUtils.hasText(market.getSkillName())) {
            return market.getSkillName();
        }
        UserSkill userSkill = userSkillMapper.selectOne(
                new LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getBizNo, skill)
                        .eq(UserSkill::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode())
                        .last("LIMIT 1"));
        if (userSkill != null && StringUtils.hasText(userSkill.getSkillName())) {
            return userSkill.getSkillName();
        }
        return skill;
    }

    private String normalizeStyleOverrides(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DEFAULT_STYLE_OVERRIDES_JSON;
        }
        try {
            objectMapper.readTree(raw);
            return raw;
        } catch (JsonProcessingException e) {
            return DEFAULT_STYLE_OVERRIDES_JSON;
        }
    }

    private Object parseStyleOverrides(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String toTagsJson(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<String> parseTags(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String toAiDetectReportJson(com.aichuangzuo.shared.vo.AiDetectReport report) {
        if (report == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            log.warn("AI 检测报告序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private com.aichuangzuo.shared.vo.AiDetectReport parseAiDetectReport(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, com.aichuangzuo.shared.vo.AiDetectReport.class);
        } catch (JsonProcessingException e) {
            log.warn("AI 检测报告解析失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Long monthlyCount(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getUserId, userId)
                .eq(Article::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode())
                .ge(Article::getCompletedAt, start)
                .lt(Article::getCompletedAt, end);
        return articleMapper.selectCount(wrapper);
    }

    private String generateBizNo() {
        return "A" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @Override
    public String generateExportToken(Long userId, String bizNo) {
        mustFind(userId, bizNo);
        return jwtUtil.generateExportToken(bizNo);
    }

    @Override
    public String parseExportToken(String token) {
        return jwtUtil.parseExportToken(token);
    }

    @Override
    public byte[] exportAsWord(String bizNo) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getBizNo, bizNo)
                .eq(Article::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode()));
        if (article == null) {
            throw new BusinessException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }
        String title = StringUtils.hasText(article.getTitle()) ? article.getTitle() : "未命名文章";
        String body = article.getBody() == null ? "" : article.getBody();
        String html = buildWordHtml(title, body);
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + bytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(bytes, 0, result, bom.length, bytes.length);
        return result;
    }

    private String buildWordHtml(String title, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" xmlns=\"http://www.w3.org/1999/xhtml\">");
        sb.append("<head><meta charset=\"UTF-8\"><title>").append(escapeHtml(title)).append("</title></head>");
        sb.append("<body style=\"font-family:-apple-system,BlinkMacSystemFont,sans-serif;padding:40px;color:#262626;\">");
        sb.append("<h1 style=\"font-size:24px;margin-bottom:16px;line-height:1.4;color:#1a1a1a;\">").append(escapeHtml(title)).append("</h1>");
        sb.append("<div style=\"font-size:16px;line-height:1.8;\">");

        String[] paragraphs = body.split("\\n\\n+");
        for (String part : paragraphs) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Matcher matcher = MARKDOWN_HEADING_PATTERN.matcher(trimmed);
            if (matcher.find()) {
                int level = Math.min(matcher.group(1).length(), 3);
                int fontSize = level == 1 ? 24 : (level == 2 ? 20 : 18);
                String heading = matcher.group(2);
                sb.append("<h").append(level)
                        .append(" style=\"font-size:").append(fontSize).append("px;font-weight:600;margin:18px 0 8px;color:#1a1a1a;\">")
                        .append(escapeHtml(heading)).append("</h").append(level).append(">");
            } else {
                sb.append("<p style=\"margin-bottom:16px;\">").append(escapeHtml(trimmed).replace("\n", "<br>")).append("</p>");
            }
        }

        sb.append("</div></body></html>");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
