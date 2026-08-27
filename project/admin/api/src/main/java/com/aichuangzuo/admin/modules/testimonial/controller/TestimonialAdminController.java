package com.aichuangzuo.admin.modules.testimonial.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialCreateRequest;
import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialStatusRequest;
import com.aichuangzuo.admin.modules.testimonial.dto.request.TestimonialUpdateRequest;
import com.aichuangzuo.admin.modules.testimonial.exception.TestimonialErrorCode;
import com.aichuangzuo.admin.modules.testimonial.service.TestimonialService;
import com.aichuangzuo.admin.modules.testimonial.util.TestimonialExcelImportUtil;
import com.aichuangzuo.admin.modules.testimonial.vo.TestimonialImportResultVO;
import com.aichuangzuo.admin.modules.testimonial.vo.TestimonialVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "首页用户评价管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/testimonials")
@RequiredArgsConstructor
public class TestimonialAdminController {

    private final TestimonialService service;
    private final com.aichuangzuo.admin.infrastructure.storage.LocalFileStorage localFileStorage;

    @Operation(summary = "评价列表")
    @GetMapping
    public Result<List<TestimonialVO>> list() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询首页评价列表, adminUserId={}", adminUserId);
        return Result.success(service.list());
    }

    @Operation(summary = "新增评价")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody TestimonialCreateRequest req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员新增首页评价, adminUserId={}, name={}", adminUserId, req.getName());
        return Result.success(service.create(req));
    }

    @Operation(summary = "更新评价")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TestimonialUpdateRequest req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新首页评价, adminUserId={}, testimonialId={}, name={}", adminUserId, id, req.getName());
        service.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除评价")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员删除首页评价, adminUserId={}, testimonialId={}", adminUserId, id);
        service.delete(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用评价")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody TestimonialStatusRequest req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新首页评价状态, adminUserId={}, testimonialId={}, isEnabled={}", adminUserId, id, req.getIsEnabled());
        service.updateStatus(id, req);
        return Result.success();
    }

    @Operation(summary = "上传评价头像")
    @PostMapping("/upload-avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员上传评价头像, adminUserId={}", adminUserId);
        String url = localFileStorage.storeTestimonialAvatar(file);
        return Result.success(url);
    }

    @Operation(summary = "下载评价导入模板")
    @GetMapping("/import-template")
    public void downloadImportTemplate(HttpServletResponse response) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员下载评价导入模板, adminUserId={}", adminUserId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String filename = URLEncoder.encode("评价导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");
        try {
            TestimonialExcelImportUtil.writeTemplate(response.getOutputStream());
        } catch (IOException e) {
            throw new BusinessException(TestimonialErrorCode.EXCEL_PARSE_ERROR);
        }
    }

    @Operation(summary = "从 Excel 批量导入评价")
    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<TestimonialImportResultVO> importExcel(@RequestParam("file") MultipartFile file) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员批量导入评价, adminUserId={}, fileName={}, fileSize={}",
                adminUserId, file.getOriginalFilename(), file.getSize());
        return Result.success(service.importFromExcel(file));
    }
}
