package com.aichuangzuo.user.modules.benefit.service;

import com.aichuangzuo.user.modules.benefit.entity.Benefit;
import com.aichuangzuo.user.modules.benefit.entity.PlanBenefit;
import com.aichuangzuo.user.modules.benefit.mapper.BenefitMapper;
import com.aichuangzuo.user.modules.benefit.mapper.PlanBenefitMapper;
import com.aichuangzuo.user.modules.benefit.service.impl.PlanCatalogServiceImpl;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.benefit.vo.NewcomerOfferVO;
import com.aichuangzuo.user.modules.benefit.vo.PlanCatalogVO;
import com.aichuangzuo.user.modules.membership.entity.Plan;
import com.aichuangzuo.user.modules.membership.mapper.PlanMapper;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PlanCatalogServiceImpl 纯单测：mock 三表读取，验证渲染逻辑。
 */
class PlanCatalogServiceImplTest {

    private PlanMapper planMapper;
    private BenefitMapper benefitMapper;
    private PlanBenefitMapper planBenefitMapper;
    private UserMembershipMapper userMembershipMapper;
    private UserInviteRelationMapper userInviteRelationMapper;
    private PlanCatalogServiceImpl service;

    @BeforeEach
    void setUp() {
        planMapper = mock(PlanMapper.class);
        benefitMapper = mock(BenefitMapper.class);
        planBenefitMapper = mock(PlanBenefitMapper.class);
        userMembershipMapper = mock(UserMembershipMapper.class);
        userInviteRelationMapper = mock(UserInviteRelationMapper.class);
        service = new PlanCatalogServiceImpl(planMapper, benefitMapper, planBenefitMapper,
                userMembershipMapper, userInviteRelationMapper, new ObjectMapper());
    }

    @Test
    void getCatalog_threePlansAndBenefits_renderedAsCardsAndCompareTable() {
        when(planMapper.selectList(any())).thenReturn(plans());
        when(benefitMapper.selectList(any())).thenReturn(benefits());
        when(planBenefitMapper.selectList(any())).thenReturn(planBenefits());

        PlanCatalogVO vo = service.getCatalog();

        assertEquals(3, vo.getPlans().size());
        assertEquals(17, vo.getCompareRows().size());

        // pro 推荐位
        PlanCatalogVO.PlanVO pro = vo.getPlans().get(1);
        assertEquals("pro", pro.getKey());
        assertEquals("专业版", pro.getName());
        assertTrue(pro.isRecommended());

        // 月度价：basic 29.9 / pro 59.9 / flagship 99.9
        assertEquals(new BigDecimal("29.90"), vo.getPlans().get(0).getMonthly().getCurrent());
        assertEquals(new BigDecimal("59.90"), pro.getMonthly().getCurrent());
        assertEquals(new BigDecimal("99.90"), vo.getPlans().get(2).getMonthly().getCurrent());

        // 年付立省 pro=215.6
        assertEquals(new BigDecimal("215.60"), pro.getYear().getSavings());
    }

    @Test
    void getCatalog_quotaCardRendersValueTpl() {
        when(planMapper.selectList(any())).thenReturn(plans());
        when(benefitMapper.selectList(any())).thenReturn(benefits());
        when(planBenefitMapper.selectList(any())).thenReturn(planBenefits());

        PlanCatalogVO.PlanVO basic = service.getCatalog().getPlans().get(0);

        // ai_article_quota: {value} 篇/月 → "30 篇/月"
        assertFeature(basic, "ai_article_quota", "30 篇/月", true);
        // sticker_quota: {value} 张/月 → "5 张/月"
        assertFeature(basic, "sticker_quota", "5 张/月", true);
        // queue_max_tasks: 队列最多 {value} 个任务 → "队列最多 1 个任务"
        assertFeature(basic, "queue_max_tasks", "队列最多 1 个任务", true);
        // ai_title_optimize: boolean=false → 显示名称，灰显
        assertFeature(basic, "ai_title_optimize", "AI 标题优化", false);
        // style_market_publish: 0 → 显示 label，不包含
        assertFeature(basic, "style_market_publish", "发布到风格市场", false);
        // style_learn_analyze: 0 → 显示 label，不包含
        assertFeature(basic, "style_learn_analyze", "学习我的风格", false);
    }

    @Test
    void getCatalog_quotaAndTierUsesValueLabelJson() {
        when(planMapper.selectList(any())).thenReturn(plans());
        when(benefitMapper.selectList(any())).thenReturn(benefits());
        when(planBenefitMapper.selectList(any())).thenReturn(planBenefits());

        PlanCatalogVO vo = service.getCatalog();
        PlanCatalogVO.PlanVO basic = vo.getPlans().get(0);
        PlanCatalogVO.PlanVO pro = vo.getPlans().get(1);
        PlanCatalogVO.PlanVO flagship = vo.getPlans().get(2);

        // style_custom 已改为 quota：1 / 2 / 4 个
        assertFeature(basic, "style_custom", "1 个", true);
        assertFeature(pro, "style_custom", "2 个", true);
        assertFeature(flagship, "style_custom", "4 个", true);

        // history_days pro=-1 → "永久"
        assertFeature(pro, "history_days", "永久", true);
        // queue_priority pro=priority → "优先"
        assertFeature(pro, "queue_priority", "优先", true);
        // template_access pro=all_20 → "全部 20+"
        assertFeature(pro, "template_access", "全部 20+", true);
    }

    @Test
    void getCatalog_compareRowCellsRespectType() {
        when(planMapper.selectList(any())).thenReturn(plans());
        when(benefitMapper.selectList(any())).thenReturn(benefits());
        when(planBenefitMapper.selectList(any())).thenReturn(planBenefits());

        List<PlanCatalogVO.CompareRowVO> rows = service.getCatalog().getCompareRows();

        // ai_article_quota：basic="30 篇/月" / pro="100 篇/月" / flagship="300 篇/月"
        PlanCatalogVO.CompareRowVO quota = findRow(rows, "ai_article_quota");
        assertEquals("30 篇/月", quota.getBasic().getValue());
        assertEquals("100 篇/月", quota.getPro().getValue());
        assertEquals("300 篇/月", quota.getFlagship().getValue());

        // ai_title_optimize：basic=false(布尔) / pro=true / flagship=true
        PlanCatalogVO.CompareRowVO title = findRow(rows, "ai_title_optimize");
        assertEquals(Boolean.FALSE, title.getBasic().getValue());
        assertEquals(Boolean.TRUE, title.getPro().getValue());
        assertEquals(Boolean.TRUE, title.getFlagship().getValue());

        // style_market_publish：basic=0 → false（quota=0 视为不包含）；pro/flagship → 字符串 + 名词“风格”
        PlanCatalogVO.CompareRowVO publish = findRow(rows, "style_market_publish");
        assertEquals(Boolean.FALSE, publish.getBasic().getValue());
        assertEquals("每月可发布 1 个风格", publish.getPro().getValue());
        assertEquals("每月可发布 2 个风格", publish.getFlagship().getValue());

        // style_learn_analyze：basic=0 → false；pro/flagship → 字符串 + "AI 风格分析"
        PlanCatalogVO.CompareRowVO learn = findRow(rows, "style_learn_analyze");
        assertEquals(Boolean.FALSE, learn.getBasic().getValue());
        assertEquals("每月可学习 1 次 AI 风格分析", learn.getPro().getValue());
        assertEquals("每月可学习 2 次 AI 风格分析", learn.getFlagship().getValue());

        // history_days：basic=30 → "30 天"；pro/flagship=-1 → "永久"
        PlanCatalogVO.CompareRowVO history = findRow(rows, "history_days");
        assertEquals("30 天", history.getBasic().getValue());
        assertEquals("永久", history.getPro().getValue());
        assertEquals("永久", history.getFlagship().getValue());
    }

    @Test
    void getCatalog_skipsBenefitsWithoutDisplayLabel() {
        // 假设某条 benefit 没有 displayLabel，应被跳过
        Benefit unconfigured = new Benefit();
        unconfigured.setCode("hidden_benefit");
        unconfigured.setName("未配置");
        unconfigured.setType("boolean");
        unconfigured.setSortOrder(99);
        unconfigured.setStatus(1);
        List<Benefit> benefitList = new ArrayList<>(benefits());
        benefitList.add(unconfigured);
        when(planMapper.selectList(any())).thenReturn(plans());
        when(benefitMapper.selectList(any())).thenReturn(benefitList);
        when(planBenefitMapper.selectList(any())).thenReturn(planBenefits());

        PlanCatalogVO vo = service.getCatalog();

        assertFalse(vo.getCompareRows().stream().anyMatch(r -> "hidden_benefit".equals(r.getCode())));
        assertFalse(vo.getPlans().get(0).getFeatures().stream().anyMatch(f -> "hidden_benefit".equals(f.getCode())));
    }

    @Test
    void getNewcomerOffer_noMembershipNoInvite_returnsDiscountedPrice() {
        when(userMembershipMapper.selectByUserId(1L)).thenReturn(null);
        when(userInviteRelationMapper.selectByInviteeId(1L)).thenReturn(null);
        when(planMapper.selectOne(any())).thenReturn(flagshipPlan());

        NewcomerOfferVO vo = service.getNewcomerOffer(1L);

        assertTrue(vo.isEligible());
        assertEquals("flagship", vo.getPlanKey());
        assertEquals("year", vo.getCycle());
        assertEquals(new BigDecimal("1198.80"), vo.getOriginalPrice());
        assertEquals(new BigDecimal("839.20"), vo.getRegularPrice());
        assertEquals(new BigDecimal("671.36"), vo.getFinalPrice());
        assertEquals(new BigDecimal("527.44"), vo.getSavings());
    }

    @Test
    void getNewcomerOffer_hasActiveMembership_returnsNotEligible() {
        UserMembership membership = new UserMembership();
        membership.setUserId(2L);
        membership.setExpiresAt(LocalDate.now().plusDays(30));
        when(userMembershipMapper.selectByUserId(2L)).thenReturn(membership);

        NewcomerOfferVO vo = service.getNewcomerOffer(2L);

        assertFalse(vo.isEligible());
    }

    @Test
    void getNewcomerOffer_hasInviteRelation_returnsNotEligible() {
        when(userMembershipMapper.selectByUserId(3L)).thenReturn(null);
        UserInviteRelation relation = new UserInviteRelation();
        relation.setInviteeId(3L);
        relation.setInviterId(4L);
        when(userInviteRelationMapper.selectByInviteeId(3L)).thenReturn(relation);

        NewcomerOfferVO vo = service.getNewcomerOffer(3L);

        assertFalse(vo.isEligible());
    }

    // ── helpers ──

    private void assertFeature(PlanCatalogVO.PlanVO plan, String code, String expectedText, boolean expectedIncluded) {
        PlanCatalogVO.FeatureVO feature = plan.getFeatures().stream()
                .filter(f -> code.equals(f.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 feature " + code));
        assertEquals(expectedText, feature.getText(), "feature " + code + " 文本");
        assertEquals(expectedIncluded, feature.isIncluded(), "feature " + code + " included");
    }

    private PlanCatalogVO.CompareRowVO findRow(List<PlanCatalogVO.CompareRowVO> rows, String code) {
        return rows.stream()
                .filter(r -> code.equals(r.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 compare row " + code));
    }

    private Plan flagshipPlan() {
        return plan("flagship", "旗舰版", 3, false,
                "99.90", "269.70", "839.20", null, "299.70", "1198.80",
                "300 篇 AI 文章/月", "900 篇 AI 文章/季", "3600 篇 AI 文章/年",
                "359.60");
    }

    private List<Plan> plans() {
        List<Plan> list = new ArrayList<>();
        list.add(plan("basic", "基础版", 1, false,
                "29.90", "80.70", "251.20", null, "89.70", "358.80",
                "30 篇 AI 文章/月", "90 篇 AI 文章/季", "360 篇 AI 文章/年",
                "107.60"));
        list.add(plan("pro", "专业版", 2, true,
                "59.90", "161.70", "503.20", null, "179.70", "718.80",
                "100 篇 AI 文章/月", "300 篇 AI 文章/季", "1200 篇 AI 文章/年",
                "215.60"));
        list.add(plan("flagship", "旗舰版", 3, false,
                "99.90", "269.70", "839.20", null, "299.70", "1198.80",
                "300 篇 AI 文章/月", "900 篇 AI 文章/季", "3600 篇 AI 文章/年",
                "359.60"));
        return list;
    }

    private Plan plan(String key, String name, int sort, boolean recommended,
            String pm, String pq, String py,
            String om, String oq, String oy,
            String am, String aq, String ay,
            String sy) {
        Plan p = new Plan();
        p.setPlanKey(key);
        p.setDisplayName(name);
        p.setSortOrder(sort);
        p.setRecommended(recommended ? 1 : 0);
        p.setPriceMonthly(new BigDecimal(pm));
        p.setPriceQuarter(new BigDecimal(pq));
        p.setPriceYear(new BigDecimal(py));
        p.setOriginalMonthly(om == null ? null : new BigDecimal(om));
        p.setOriginalQuarter(oq == null ? null : new BigDecimal(oq));
        p.setOriginalYear(oy == null ? null : new BigDecimal(oy));
        p.setArticlesMonthly(am);
        p.setArticlesQuarter(aq);
        p.setArticlesYear(ay);
        p.setSavingsYear(new BigDecimal(sy));
        p.setStatus(1);
        return p;
    }

    private List<Benefit> benefits() {
        List<Benefit> list = new ArrayList<>();
        list.add(benefit("ai_article_quota", "AI 文章生成", "quota", "AI 文章生成", "{value} 篇/月", null, 1));
        list.add(benefit("export_word", "导出 Word", "boolean", "导出 Word", null, null, 2));
        list.add(benefit("copy_text", "复制正文", "boolean", "复制正文", null, null, 3));
        list.add(benefit("ai_topic", "AI 选题灵感", "boolean", "AI 选题灵感", null, null, 4));
        list.add(benefit("ai_title_optimize", "AI 标题优化", "boolean", "AI 标题优化", null, null, 5));
        list.add(benefit("online_edit", "在线编辑", "boolean", "在线编辑", null, null, 6));
        list.add(benefit("style_custom", "我的风格数量", "quota", "我的风格", "{value} 个", null, 7));
        list.add(benefit("seo_keywords", "SEO 关键词建议", "boolean", "SEO 关键词建议", null, null, 8));
        list.add(benefit("template_access", "文章模板", "tier", "文章模板", null,
                "{\"basic_8\":\"8 款基础\",\"all_20\":\"全部 20+\",\"all_custom\":\"全部 + 自定义\"}", 9));
        list.add(benefit("sticker_quota", "贴图生成", "quota", "贴图生成", "{value} 张/月", null, 10));
        list.add(benefit("batch_generate", "批量生成/改写", "boolean", "批量生成/改写", null, null, 11));
        list.add(benefit("batch_export", "批量导出", "boolean", "批量导出", null, null, 12));
        list.add(benefit("history_days", "历史记录", "quota", "历史记录", null,
                "{\"30\":\"30 天\",\"90\":\"90 天\",\"180\":\"180 天\",\"365\":\"365 天\",\"-1\":\"永久\"}", 13));
        list.add(benefit("queue_priority", "生成队列优先级", "tier", "生成队列优先级", null,
                "{\"standard\":\"标准\",\"priority\":\"优先\",\"express\":\"极速\"}", 14));
        list.add(benefit("queue_max_tasks", "队列任务数", "quota", "队列任务数", "队列最多 {value} 个任务", null, 15));
        list.add(benefit("style_market_publish", "发布到风格市场", "quota", "发布到风格市场", "每月可发布 {value} 个风格", null, 16));
        list.add(benefit("style_learn_analyze", "学习我的风格", "quota", "学习我的风格", "每月可学习 {value} 次 AI 风格分析", null, 17));
        return list;
    }

    private Benefit benefit(String code, String name, String type, String displayLabel,
            String cardValueTpl, String valueLabelJson, int sort) {
        Benefit b = new Benefit();
        b.setCode(code);
        b.setName(name);
        b.setType(type);
        b.setDisplayLabel(displayLabel);
        b.setCardValueTpl(cardValueTpl);
        b.setValueLabelJson(valueLabelJson);
        b.setSortOrder(sort);
        b.setStatus(1);
        return b;
    }

    private List<PlanBenefit> planBenefits() {
        List<PlanBenefit> list = new ArrayList<>();
        list.add(pb("basic", "ai_article_quota", "30"));
        list.add(pb("pro", "ai_article_quota", "100"));
        list.add(pb("flagship", "ai_article_quota", "300"));
        list.add(pb("basic", "export_word", "true"));
        list.add(pb("pro", "export_word", "true"));
        list.add(pb("flagship", "export_word", "true"));
        list.add(pb("basic", "copy_text", "true"));
        list.add(pb("pro", "copy_text", "true"));
        list.add(pb("flagship", "copy_text", "true"));
        list.add(pb("basic", "ai_topic", "true"));
        list.add(pb("pro", "ai_topic", "true"));
        list.add(pb("flagship", "ai_topic", "true"));
        list.add(pb("basic", "ai_title_optimize", "false"));
        list.add(pb("pro", "ai_title_optimize", "true"));
        list.add(pb("flagship", "ai_title_optimize", "true"));
        list.add(pb("basic", "online_edit", "false"));
        list.add(pb("pro", "online_edit", "true"));
        list.add(pb("flagship", "online_edit", "true"));
        list.add(pb("basic", "style_custom", "1"));
        list.add(pb("pro", "style_custom", "2"));
        list.add(pb("flagship", "style_custom", "4"));
        list.add(pb("basic", "seo_keywords", "false"));
        list.add(pb("pro", "seo_keywords", "false"));
        list.add(pb("flagship", "seo_keywords", "true"));
        list.add(pb("basic", "template_access", "basic_8"));
        list.add(pb("pro", "template_access", "all_20"));
        list.add(pb("flagship", "template_access", "all_custom"));
        list.add(pb("basic", "sticker_quota", "5"));
        list.add(pb("pro", "sticker_quota", "30"));
        list.add(pb("flagship", "sticker_quota", "100"));
        list.add(pb("basic", "batch_generate", "false"));
        list.add(pb("pro", "batch_generate", "false"));
        list.add(pb("flagship", "batch_generate", "true"));
        list.add(pb("basic", "batch_export", "false"));
        list.add(pb("pro", "batch_export", "false"));
        list.add(pb("flagship", "batch_export", "true"));
        list.add(pb("basic", "history_days", "30"));
        list.add(pb("pro", "history_days", "-1"));
        list.add(pb("flagship", "history_days", "-1"));
        list.add(pb("basic", "queue_priority", "standard"));
        list.add(pb("pro", "queue_priority", "priority"));
        list.add(pb("flagship", "queue_priority", "express"));
        list.add(pb("basic", "queue_max_tasks", "1"));
        list.add(pb("pro", "queue_max_tasks", "5"));
        list.add(pb("flagship", "queue_max_tasks", "10"));
        list.add(pb("basic", "style_market_publish", "0"));
        list.add(pb("pro", "style_market_publish", "1"));
        list.add(pb("flagship", "style_market_publish", "2"));
        list.add(pb("basic", "style_learn_analyze", "0"));
        list.add(pb("pro", "style_learn_analyze", "1"));
        list.add(pb("flagship", "style_learn_analyze", "2"));
        return list;
    }

    private PlanBenefit pb(String plan, String code, String value) {
        PlanBenefit pb = new PlanBenefit();
        pb.setPlanKey(plan);
        pb.setBenefitCode(code);
        pb.setBenefitValue(value);
        return pb;
    }
}