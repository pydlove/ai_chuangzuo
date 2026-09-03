package com.aichuangzuo.user.modules.article.service.impl;

import com.aichuangzuo.user.infrastructure.security.JwtUtil;
import com.aichuangzuo.user.modules.article.dto.request.SaveArticleRequest;
import com.aichuangzuo.user.modules.article.dto.request.UpdateArticleRequest;
import com.aichuangzuo.user.modules.article.entity.Article;
import com.aichuangzuo.shared.enums.error.ArticleErrorCode;
import com.aichuangzuo.user.modules.article.mapper.ArticleMapper;
import com.aichuangzuo.user.modules.exporttemplate.entity.ExportTemplate;
import com.aichuangzuo.user.modules.exporttemplate.mapper.ExportTemplateMapper;
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
import org.apache.poi.xwpf.usermodel.LineSpacingRule;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final ExportTemplateMapper exportTemplateMapper;

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
        ExportTemplate template = null;
        if (StringUtils.hasText(article.getTemplate())) {
            template = exportTemplateMapper.selectOne(new LambdaQueryWrapper<ExportTemplate>()
                    .eq(ExportTemplate::getTemplateKey, article.getTemplate())
                    .eq(ExportTemplate::getStatus, 1));
        }
        return buildWordDocument(article, template);
    }

    /**
     * 按文章关联的导出模板 visual_style_json 渲染 Word 文档（.docx），
     * 与前端 PreviewIndex / WorksIndex 的导出样式保持一致。
     *
     * <p>当前未处理编辑页保存的 styleOverrides（加粗/颜色/对齐等），
     * 如需完全同步可后续把 overrides 一并传入并解析。</p>
     */
    private byte[] buildWordDocument(Article article, ExportTemplate template) {
        Map<String, Object> style = parseVisualStyle(template != null ? template.getVisualStyleJson() : null);
        String title = StringUtils.hasText(article.getTitle()) ? article.getTitle() : "未命名文章";
        String body = article.getBody() == null ? "" : article.getBody();
        body = stripLeadingTitle(body, title.trim());
        String signatureText = template != null ? template.getSignatureText() : null;
        String signaturePosition = template != null ? template.getSignaturePosition() : null;
        body = stripSignature(body, signatureText);

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            appendTitle(document, title, style);

            if ("start".equals(signaturePosition) && StringUtils.hasText(signatureText)) {
                appendSignature(document, signatureText, style);
            }
            appendBody(document, body, style);
            if (!"start".equals(signaturePosition) && StringUtils.hasText(signatureText)) {
                appendSignature(document, signatureText, style);
            }

            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("生成 Word 文档失败, bizNo={}", article.getBizNo(), e);
            throw new BusinessException(ArticleErrorCode.ARTICLE_EXPORT_FAILED);
        }
    }

    private Map<String, Object> parseVisualStyle(String json) {
        Map<String, Object> style = new HashMap<>();
        if (!StringUtils.hasText(json)) {
            return style;
        }
        try {
            Map<?, ?> raw = objectMapper.readValue(json, Map.class);
            raw.forEach((k, v) -> style.put(String.valueOf(k), v));
        } catch (JsonProcessingException e) {
            log.warn("解析模板 visualStyleJson 失败", e);
        }
        return style;
    }

    private String styleValue(Map<String, Object> style, String key) {
        return styleValue(style, key, null);
    }

    private String styleValue(Map<String, Object> style, String key, String defaultValue) {
        Object v = style.get(key);
        if (v == null) {
            return defaultValue;
        }
        String s = v.toString();
        return StringUtils.hasText(s) ? s : defaultValue;
    }

    private void appendTitle(XWPFDocument document, String title, Map<String, Object> style) {
        XWPFParagraph paragraph = document.createParagraph();
        applyTitleParagraphStyle(paragraph, style);
        String titleIcon = styleValue(style, "titleIcon");
        if (StringUtils.hasText(titleIcon)) {
            XWPFRun iconRun = paragraph.createRun();
            applyTitleRunStyle(iconRun, style);
            iconRun.setText(titleIcon + " ");
        }
        XWPFRun run = paragraph.createRun();
        applyTitleRunStyle(run, style);
        run.setText(title);
    }

    private void applyTitleParagraphStyle(XWPFParagraph paragraph, Map<String, Object> style) {
        paragraph.setAlignment(toAlignment(styleValue(style, "titleAlign", "left")));
        paragraph.setSpacingAfter(parsePxToTwips(styleValue(style, "titleMarginBottom", "16px")));
        String background = styleValue(style, "titleBackground");
        if (background != null && isSolidColor(background)) {
            setParagraphShadingColor(paragraph, stripHash(background));
        }
        String borderBottom = styleValue(style, "titleBorderBottom");
        if (borderBottom != null) {
            setBorderBottom(paragraph, borderBottom);
        }
        String border = styleValue(style, "titleBorder");
        if (border != null) {
            setBorder(paragraph, border);
        }
    }

    private void applyTitleRunStyle(XWPFRun run, Map<String, Object> style) {
        run.setFontFamily(styleValue(style, "titleFontFamily", styleValue(style, "font", "-apple-system, BlinkMacSystemFont, sans-serif")));
        run.setFontSize(halfPoints(styleValue(style, "titleSize", "24px")));
        run.setBold(toFontWeight(styleValue(style, "titleFontWeight", "700")) >= 600);
        run.setColor(stripHash(styleValue(style, "titleColor", "#1a1a1a")));
        run.setItalic("italic".equals(styleValue(style, "titleFontStyle")));
    }

    private void appendSignature(XWPFDocument document, String signatureText, Map<String, Object> style) {
        XWPFParagraph paragraph = document.createParagraph();
        applySignatureParagraphStyle(paragraph, style);
        XWPFRun run = paragraph.createRun();
        applySignatureRunStyle(run, style);
        setRunText(run, signatureText);
    }

    private void applySignatureParagraphStyle(XWPFParagraph paragraph, Map<String, Object> style) {
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingBefore(twips("32px"));
        setBorderTop(paragraph, "1px solid " + styleValue(style, "metaBorder", "#eee"));
    }

    private void applySignatureRunStyle(XWPFRun run, Map<String, Object> style) {
        run.setColor(stripHash(styleValue(style, "metaColor", "#8c8c8c")));
        run.setFontSize(halfPoints("13px"));
    }

    private void appendBody(XWPFDocument document, String body, Map<String, Object> style) {
        String[] paragraphs = body.split("\\n\\n+");
        for (String part : paragraphs) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Matcher matcher = MARKDOWN_HEADING_PATTERN.matcher(trimmed);
            if (matcher.find()) {
                int level = Math.min(matcher.group(1).length(), 3);
                String heading = matcher.group(2);
                appendHeading(document, heading, level, style);
            } else if (trimmed.matches("^【[^】]+】$")) {
                String heading = trimmed.substring(1, trimmed.length() - 1);
                appendHeading(document, heading, 2, style);
            } else if (trimmed.startsWith("> ")) {
                appendCallout(document, trimmed.substring(2), style);
            } else {
                appendBodyParagraph(document, trimmed, style);
            }
        }
    }

    private void appendHeading(XWPFDocument document, String heading, int level, Map<String, Object> style) {
        XWPFParagraph paragraph = document.createParagraph();
        applyHeadingParagraphStyle(paragraph, style);
        XWPFRun run = paragraph.createRun();
        applyHeadingRunStyle(run, style);
        setRunText(run, heading);
    }

    private void applyHeadingParagraphStyle(XWPFParagraph paragraph, Map<String, Object> style) {
        paragraph.setAlignment(toAlignment(styleValue(style, "headingAlign", "left")));
        String margin = styleValue(style, "headingMargin");
        if (margin != null) {
            String[] parts = margin.split("\\s+");
            if (parts.length >= 1) paragraph.setSpacingBefore(parsePxToTwips(parts[0]));
            if (parts.length >= 3) paragraph.setSpacingAfter(parsePxToTwips(parts[2]));
        } else {
            paragraph.setSpacingBefore(twips("18px"));
            paragraph.setSpacingAfter(twips("8px"));
        }
        String background = styleValue(style, "headingBackground");
        if (background != null && isSolidColor(background)) {
            setParagraphShadingColor(paragraph, stripHash(background));
        }
        String headingBorder = styleValue(style, "headingBorder");
        if (headingBorder != null && !"none".equals(headingBorder)) {
            setBorderLeft(paragraph, headingBorder);
            Object headingPl = style.get("headingPl");
            int pl = headingPl instanceof Number ? ((Number) headingPl).intValue() : 0;
            paragraph.setIndentationLeft(twips(pl + "px"));
        }
        String headingBorderBottom = styleValue(style, "headingBorderBottom");
        if (headingBorderBottom != null) {
            setBorderBottom(paragraph, headingBorderBottom);
        }
    }

    private void applyHeadingRunStyle(XWPFRun run, Map<String, Object> style) {
        run.setFontFamily(styleValue(style, "headingFontFamily", styleValue(style, "font", "-apple-system, BlinkMacSystemFont, sans-serif")));
        run.setFontSize(halfPoints(styleValue(style, "headingSize", "18px")));
        run.setBold(toFontWeight(styleValue(style, "headingFontWeight", "600")) >= 600);
        run.setColor(stripHash(styleValue(style, "headingColor", "#1a1a1a")));
    }

    private void appendCallout(XWPFDocument document, String text, Map<String, Object> style) {
        XWPFParagraph paragraph = document.createParagraph();
        applyCalloutParagraphStyle(paragraph, style);
        XWPFRun run = paragraph.createRun();
        applyCalloutRunStyle(run, style);
        setRunText(run, text);
    }

    private void applyCalloutParagraphStyle(XWPFParagraph paragraph, Map<String, Object> style) {
        String variant = styleValue(style, "calloutVariant", "default");
        paragraph.setSpacingBefore(twips("14px"));
        paragraph.setSpacingAfter(twips("14px"));
        switch (variant) {
            case "pill" -> {
                setParagraphShadingColor(paragraph, stripHash(styleValue(style, "calloutBg", "#fff0f2")));
                setBorder(paragraph, "1px solid " + styleValue(style, "calloutBg", "#fff0f2"));
            }
            case "card" -> {
                setParagraphShadingColor(paragraph, stripHash(styleValue(style, "calloutBg", "#fff")));
                setBorderLeft(paragraph, "3px solid " + styleValue(style, "headingColor", "#07c160"));
            }
            case "cta" -> {
                paragraph.setAlignment(ParagraphAlignment.CENTER);
                setParagraphShadingColor(paragraph, stripHash(styleValue(style, "calloutBg", "#fff")));
                setBorder(paragraph, "2px solid " + styleValue(style, "calloutColor", "#cf1322"));
            }
            case "checklist" -> {
                setParagraphShadingColor(paragraph, stripHash(styleValue(style, "calloutBg", "#f6ffed")));
            }
            default -> {
                setParagraphShadingColor(paragraph, stripHash(styleValue(style, "calloutBg", "#f6ffed")));
                String border = styleValue(style, "calloutBorder");
                if (border != null && !"none".equals(border)) {
                    setBorderLeft(paragraph, border);
                }
            }
        }
    }

    private void applyCalloutRunStyle(XWPFRun run, Map<String, Object> style) {
        String variant = styleValue(style, "calloutVariant", "default");
        run.setFontSize(halfPoints("13px"));
        String color = switch (variant) {
            case "pill" -> styleValue(style, "calloutColor", "#ff2442");
            default -> styleValue(style, "calloutColor", "#262626");
        };
        run.setColor(stripHash(color));
        if ("cta".equals(variant)) {
            run.setBold(true);
        }
    }

    private void appendBodyParagraph(XWPFDocument document, String text, Map<String, Object> style) {
        XWPFParagraph paragraph = document.createParagraph();
        applyBodyParagraphStyle(paragraph, style);
        XWPFRun run = paragraph.createRun();
        applyBodyRunStyle(run, style);
        setRunText(run, text);
    }

    private void applyBodyParagraphStyle(XWPFParagraph paragraph, Map<String, Object> style) {
        paragraph.setAlignment(toAlignment(styleValue(style, "bodyAlign", "left")));
        paragraph.setSpacingAfter(twips("16px"));
        double lineHeight = parseDouble(styleValue(style, "bodyLine", "1.8"));
        paragraph.setSpacingBetween(lineHeight, LineSpacingRule.AUTO);
    }

    private void applyBodyRunStyle(XWPFRun run, Map<String, Object> style) {
        run.setFontFamily(styleValue(style, "font", "-apple-system, BlinkMacSystemFont, sans-serif"));
        run.setFontSize(halfPoints(styleValue(style, "bodySize", "16px")));
        run.setColor(stripHash(styleValue(style, "bodyColor", "#262626")));
    }

    private static void setRunText(XWPFRun run, String text) {
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            run.setText(lines[i]);
            if (i < lines.length - 1) {
                run.addBreak();
            }
        }
    }

    private static ParagraphAlignment toAlignment(String align) {
        if (align == null) return ParagraphAlignment.LEFT;
        return switch (align) {
            case "center" -> ParagraphAlignment.CENTER;
            case "right" -> ParagraphAlignment.RIGHT;
            case "justify" -> ParagraphAlignment.BOTH;
            default -> ParagraphAlignment.LEFT;
        };
    }

    private static int halfPoints(String cssSize) {
        int px = parsePx(cssSize);
        return px * 3 / 2;
    }

    private static int twips(String cssSize) {
        return parsePxToTwips(cssSize);
    }

    private static int parsePxToTwips(String cssSize) {
        int px = parsePx(cssSize);
        return px * 15;
    }

    private static int parsePx(String cssSize) {
        if (cssSize == null) return 0;
        String s = cssSize.trim().toLowerCase();
        if (s.endsWith("px")) {
            s = s.substring(0, s.length() - 2).trim();
        }
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDouble(String value) {
        if (value == null) return 1.8;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 1.8;
        }
    }

    private static String stripHash(String color) {
        if (color == null) return "000000";
        return color.startsWith("#") ? color.substring(1) : color;
    }

    private static boolean isSolidColor(String color) {
        return color != null && !color.contains("gradient");
    }

    private static int toFontWeight(String weight) {
        if (weight == null) return 400;
        try {
            return Integer.parseInt(weight.trim());
        } catch (NumberFormatException e) {
            return "bold".equalsIgnoreCase(weight) ? 700 : 400;
        }
    }

    private static void setBorderLeft(XWPFParagraph paragraph, String cssBorder) {
        setBorder(paragraph, "left", cssBorder);
    }

    private static void setBorderBottom(XWPFParagraph paragraph, String cssBorder) {
        setBorder(paragraph, "bottom", cssBorder);
    }

    private static void setBorderTop(XWPFParagraph paragraph, String cssBorder) {
        setBorder(paragraph, "top", cssBorder);
    }

    private static void setBorder(XWPFParagraph paragraph, String cssBorder) {
        setBorder(paragraph, "all", cssBorder);
    }

    private static void setBorder(XWPFParagraph paragraph, String side, String cssBorder) {
        String[] parts = cssBorder.trim().split("\\s+");
        if (parts.length < 3) return;
        String widthPart = parts[0];
        String colorPart = parts[parts.length - 1];
        int widthPx = parsePx(widthPart);
        int widthEighths = Math.max(1, widthPx * 8);
        String color = stripHash(colorPart);
        BigInteger size = BigInteger.valueOf(widthEighths);
        BigInteger space = BigInteger.valueOf(4);

        CTP ctp = paragraph.getCTP();
        CTPPr pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        CTPBdr pbdr = pPr.isSetPBdr() ? pPr.getPBdr() : pPr.addNewPBdr();
        CTBorder border = CTBorder.Factory.newInstance();
        border.setVal(STBorder.SINGLE);
        border.setSz(size);
        border.setColor(color);
        border.setSpace(space);

        switch (side) {
            case "left" -> pbdr.setLeft(border);
            case "bottom" -> pbdr.setBottom(border);
            case "top" -> pbdr.setTop(border);
            case "all" -> {
                pbdr.setTop(border);
                pbdr.setLeft(border);
                pbdr.setBottom(border);
                pbdr.setRight(border);
            }
        }
    }

    private static void setParagraphShadingColor(XWPFParagraph paragraph, String color) {
        CTP ctp = paragraph.getCTP();
        CTPPr pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        CTShd shd = pPr.isSetShd() ? pPr.getShd() : pPr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setFill(color);
    }

    private String stripLeadingTitle(String body, String title) {
        if (!StringUtils.hasText(body) || !StringUtils.hasText(title)) {
            return body;
        }
        String escaped = title.replaceAll("([.*+?^${}()|\\[\\]\\\\])", "\\\\$1");
        String regex = "^\\s*[#🌟【】\\s]*" + escaped + "[#🌟【】\\s]*\\n+";
        Pattern pattern = Pattern.compile(regex);
        String result = body;
        while (pattern.matcher(result).find()) {
            result = pattern.matcher(result).replaceFirst("");
        }
        return result;
    }

    private String stripSignature(String body, String signatureText) {
        if (!StringUtils.hasText(body) || !StringUtils.hasText(signatureText)) {
            return body;
        }
        String result = StringUtils.trimTrailingWhitespace(body);
        while (result.endsWith(signatureText)) {
            result = StringUtils.trimTrailingWhitespace(result.substring(0, result.length() - signatureText.length()));
        }
        String trimmedStart = StringUtils.trimLeadingWhitespace(result);
        if (trimmedStart.startsWith(signatureText)) {
            result = StringUtils.trimLeadingWhitespace(trimmedStart.substring(signatureText.length()));
        }
        return result;
    }

}
