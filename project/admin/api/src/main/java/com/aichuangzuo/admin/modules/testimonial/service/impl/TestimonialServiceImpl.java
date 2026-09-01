package com.aichuangzuo.admin.modules.testimonial.service.impl;

import com.aichuangzuo.admin.modules.testimonial.dto.excel.TestimonialImportExcelRowData;
import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialCreateRequest;
import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialPageRequest;
import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialStatusRequest;
import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialUpdateRequest;
import com.aichuangzuo.admin.modules.testimonial.entity.TestimonialEntity;
import com.aichuangzuo.admin.modules.testimonial.exception.TestimonialErrorCode;
import com.aichuangzuo.admin.modules.testimonial.mapper.TestimonialMapper;
import com.aichuangzuo.admin.modules.testimonial.service.TestimonialService;
import com.aichuangzuo.admin.modules.testimonial.util.TestimonialExcelImportUtil;
import com.aichuangzuo.admin.modules.testimonial.vo.TestimonialImportResultVO;
import com.aichuangzuo.admin.modules.testimonial.vo.TestimonialImportRowErrorVO;
import com.aichuangzuo.admin.modules.testimonial.vo.TestimonialVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestimonialServiceImpl implements TestimonialService {

    private final TestimonialMapper mapper;

    @Override
    public IPage<TestimonialVO> page(TestimonialPageRequest request) {
        Page<TestimonialEntity> pageParam = new Page<>(request.getPageNum(), request.getPageSize());
        QueryWrapper<TestimonialEntity> wrapper = new QueryWrapper<TestimonialEntity>()
                .orderByAsc("sort")
                .orderByAsc("id");
        String keyword = request.getKeyword();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("name", keyword).or().like("review_text", keyword));
        }
        Page<TestimonialEntity> result = mapper.selectPage(pageParam, wrapper);
        List<TestimonialVO> records = result.getRecords().stream()
                .map(this::toVo)
                .toList();
        Page<TestimonialVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public Long create(TestimonialCreateRequest req) {
        TestimonialEntity e = new TestimonialEntity();
        e.setAvatarUrl(req.getAvatarUrl() != null ? req.getAvatarUrl() : "");
        e.setName(req.getName());
        e.setTitle(req.getTitle() != null ? req.getTitle() : "");
        e.setStarRating(req.getStarRating());
        e.setReviewText(req.getReviewText());
        e.setSort(req.getSort() != null ? req.getSort() : 0);
        e.setIsEnabled(req.getIsEnabled() != null ? req.getIsEnabled() : 1);
        mapper.insert(e);
        return e.getId();
    }

    @Override
    public void update(Long id, TestimonialUpdateRequest req) {
        TestimonialEntity e = requireExisting(id);
        e.setAvatarUrl(req.getAvatarUrl() != null ? req.getAvatarUrl() : "");
        e.setName(req.getName());
        e.setTitle(req.getTitle() != null ? req.getTitle() : "");
        e.setStarRating(req.getStarRating());
        e.setReviewText(req.getReviewText());
        e.setSort(req.getSort() != null ? req.getSort() : 0);
        e.setIsEnabled(req.getIsEnabled() != null ? req.getIsEnabled() : 1);
        mapper.updateById(e);
    }

    @Override
    public void delete(Long id) {
        requireExisting(id);
        mapper.deleteById(id);
    }

    @Override
    public Integer batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return mapper.deleteBatchIds(ids);
    }

    @Override
    public void updateStatus(Long id, TestimonialStatusRequest req) {
        TestimonialEntity e = requireExisting(id);
        e.setIsEnabled(req.getIsEnabled());
        mapper.updateById(e);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TestimonialImportResultVO importFromExcel(MultipartFile file) {
        List<TestimonialImportExcelRowData> rows = TestimonialExcelImportUtil.readRows(file);
        List<TestimonialImportRowErrorVO> errors = new ArrayList<>();
        List<TestimonialEntity> entities = new ArrayList<>(rows.size());

        for (int i = 0; i < rows.size(); i++) {
            TestimonialImportExcelRowData row = rows.get(i);
            int rowIndex = i + 2;
            List<String> rowErrors = new ArrayList<>();
            TestimonialEntity e = validateAndBuildEntity(row, rowIndex, rowErrors);
            if (!rowErrors.isEmpty()) {
                errors.add(new TestimonialImportRowErrorVO(rowIndex, trim(row.getName()), rowErrors));
            } else {
                entities.add(e);
            }
        }

        if (!errors.isEmpty()) {
            return new TestimonialImportResultVO(false, rows.size(), 0, errors);
        }

        for (TestimonialEntity e : entities) {
            mapper.insert(e);
        }
        return new TestimonialImportResultVO(true, rows.size(), entities.size(), List.of());
    }

    private TestimonialEntity validateAndBuildEntity(TestimonialImportExcelRowData row, int rowIndex,
                                                     List<String> errors) {
        String avatarUrl = trim(row.getAvatarUrl());
        String name = trim(row.getName());
        String title = trim(row.getTitle());
        String starRatingText = trim(row.getStarRating());
        String reviewText = trim(row.getReviewText());
        String sortText = trim(row.getSort());
        String isEnabledText = trim(row.getIsEnabled());

        if (name == null || name.isEmpty()) {
            errors.add("【姓名】未填写");
        } else if (name.length() > 64) {
            errors.add("【姓名】长度超过 64 字符，当前 " + name.length() + " 字符");
        }

        if (title != null && title.length() > 128) {
            errors.add("【身份/职位】长度超过 128 字符，当前 " + title.length() + " 字符");
        }

        if (avatarUrl != null && avatarUrl.length() > 512) {
            errors.add("【头像 URL】长度超过 512 字符，当前 " + avatarUrl.length() + " 字符");
        }

        if (reviewText == null || reviewText.isEmpty()) {
            errors.add("【评价内容】未填写");
        } else if (reviewText.length() > 2048) {
            errors.add("【评价内容】长度超过 2048 字符，当前 " + reviewText.length() + " 字符");
        }

        Integer starRating = parseInteger(starRatingText, 1, 5, "星级", errors);
        Integer sort = parseIntegerWithDefault(sortText, 0, 0, Integer.MAX_VALUE, "排序", errors);
        Integer isEnabled = parseIntegerWithDefault(isEnabledText, 1, 0, 1, "启用状态", errors);

        if (!errors.isEmpty()) {
            return null;
        }

        TestimonialEntity e = new TestimonialEntity();
        e.setAvatarUrl(avatarUrl != null ? avatarUrl : "");
        e.setName(name);
        e.setTitle(title != null ? title : "");
        e.setStarRating(starRating);
        e.setReviewText(reviewText);
        e.setSort(sort);
        e.setIsEnabled(isEnabled);
        return e;
    }

    private Integer parseInteger(String text, int min, int max, String fieldName, List<String> errors) {
        if (text == null || text.isEmpty()) {
            errors.add("【" + fieldName + "】未填写");
            return null;
        }
        try {
            int value = Integer.parseInt(text);
            if (value < min || value > max) {
                errors.add("【" + fieldName + "】需在 " + min + "-" + max + " 之间");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            errors.add("【" + fieldName + "】格式不正确");
            return null;
        }
    }

    private Integer parseIntegerWithDefault(String text, int defaultValue, int min, int max,
                                          String fieldName, List<String> errors) {
        if (text == null || text.isEmpty()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(text);
            if (value < min || value > max) {
                errors.add("【" + fieldName + "】需在 " + min + "-" + max + " 之间");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            errors.add("【" + fieldName + "】格式不正确");
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private TestimonialEntity requireExisting(Long id) {
        TestimonialEntity e = mapper.selectById(id);
        if (e == null) {
            throw new BusinessException(TestimonialErrorCode.TESTIMONIAL_NOT_FOUND);
        }
        return e;
    }

    private TestimonialVO toVo(TestimonialEntity e) {
        TestimonialVO v = new TestimonialVO();
        v.setId(e.getId());
        v.setAvatarUrl(normalizeAvatarUrl(e.getAvatarUrl()));
        v.setName(e.getName());
        v.setTitle(e.getTitle());
        v.setStarRating(e.getStarRating());
        v.setReviewText(e.getReviewText());
        v.setSort(e.getSort());
        v.setIsEnabled(e.getIsEnabled());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }

    /**
     * 兼容旧版头像 URL。
     *
     * <p>早期 storeTestimonialAvatar 返回 /uploads/testimonial/avatar/...，线上 Nginx 只代理了 /api/v1/admin，
     * 导致旧头像裂图。读数据时自动把旧路径改写为 /api/v1/admin/uploads/...，新路径不受影响。
     */
    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl != null && avatarUrl.startsWith("/uploads/")) {
            return "/api/v1/admin" + avatarUrl;
        }
        return avatarUrl;
    }
}
