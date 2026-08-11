package com.aichuangzuo.admin.modules.generation.controller;

import com.aichuangzuo.admin.modules.generation.dto.request.BatchStopGenerationTaskRequest;
import com.aichuangzuo.admin.modules.generation.dto.request.GenerationTaskQueryRequest;
import com.aichuangzuo.admin.modules.generation.service.ArticleDownload;
import com.aichuangzuo.admin.modules.generation.service.GenerationTaskAdminService;
import com.aichuangzuo.admin.modules.generation.vo.BatchStopGenerationTaskResultVO;
import com.aichuangzuo.admin.modules.generation.vo.GenerationTaskAdminPageVO;
import com.aichuangzuo.admin.modules.generation.vo.GeneratedArticleVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 端-创作任务管理 API（队列页 + 运维操作）。
 */
@Tag(name = "管理端-创作队列")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/generation/tasks")
@RequiredArgsConstructor
public class GenerationTaskAdminController {

    private final GenerationTaskAdminService service;

    /**
     * 任务列表（按 status 过滤 + 关键字 + 分页）。
     */
    @GetMapping
    public Result<GenerationTaskAdminPageVO> list(GenerationTaskQueryRequest request) {
        log.info("管理端查询创作任务列表, status={}, keyword={}, page={}, pageSize={}",
                request.getStatus(), request.getKeyword(), request.getPage(), request.getPageSize());
        return Result.success(service.list(request));
    }

    /**
     * 手动停止任务：QUEUED / PROCESSING → FAILED。
     */
    @PostMapping("/{id}/stop")
    public Result<Void> stop(@PathVariable Long id) {
        log.info("管理端手动停止创作任务, taskId={}", id);
        service.stopTask(id);
        return Result.success();
    }

    /**
     * 批量停止任务：仅处理 QUEUED / PROCESSING 的任务，返回成功/不存在/状态不允许/失败的详情。
     */
    @PostMapping("/batch/stop")
    public Result<BatchStopGenerationTaskResultVO> batchStop(@RequestBody BatchStopGenerationTaskRequest request) {
        log.info("管理端批量停止创作任务, ids={}", request.getIds());
        return Result.success(service.batchStop(request.getIds()));
    }

    /**
     * 在线预览已完成任务的最终文章。
     */
    @GetMapping("/{id}/article")
    public Result<GeneratedArticleVO> previewArticle(@PathVariable Long id) {
        log.info("管理端预览创作任务文章, taskId={}", id);
        return Result.success(service.previewArticle(id));
    }

    /**
     * 下载已完成任务的最终文章（markdown 格式）。
     */
    @GetMapping("/{id}/article/download")
    public ResponseEntity<byte[]> downloadArticle(@PathVariable Long id) {
        log.info("管理端下载创作任务文章, taskId={}", id);
        ArticleDownload download = service.downloadArticle(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/markdown;charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", download.getFilename());
        return ResponseEntity.ok()
                .headers(headers)
                .body(download.getContent());
    }
}
