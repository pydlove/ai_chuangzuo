package com.aichuangzuo.admin.modules.simulation.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.earnings.vo.PageResult;
import com.aichuangzuo.admin.modules.simulation.dto.request.SimulationLibraryBatchDeleteRequest;
import com.aichuangzuo.admin.modules.simulation.dto.request.SimulationNicknameBatchAddRequest;
import com.aichuangzuo.admin.modules.simulation.service.SimulationLibraryService;
import com.aichuangzuo.admin.modules.simulation.util.SimulationNicknameExcelUtil;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationAvatarVO;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationNicknameVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 管理端-模拟运营-昵称库/头像库：上传维护，PROFILE 阶段随机取用，用后即删。
 */
@Tag(name = "管理端-模拟运营素材库")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/simulation/library")
@RequiredArgsConstructor
public class SimulationLibraryAdminController {

    private final SimulationLibraryService libraryService;
    private final AdminUserPermissionService permissionService;

    // ---------- 昵称库 ----------

    @GetMapping("/nicknames")
    public Result<PageResult<SimulationNicknameVO>> listNicknames(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size) {
        checkSuperAdmin();
        return Result.success(libraryService.listNicknames(keyword, page, size));
    }

    @GetMapping("/nicknames/count")
    public Result<Long> countNicknames() {
        checkSuperAdmin();
        return Result.success(libraryService.countNicknames());
    }

    @PostMapping("/nicknames")
    public Result<Map<String, Integer>> addNicknames(@Valid @RequestBody SimulationNicknameBatchAddRequest request) {
        checkSuperAdmin();
        int inserted = libraryService.addNicknames(request.getNicknames());
        log.info("管理员批量添加模拟昵称 inserted={}", inserted);
        return Result.success(Map.of("inserted", inserted));
    }

    @PostMapping(value = "/nicknames/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Integer>> importNicknames(@RequestPart("file") MultipartFile file) {
        checkSuperAdmin();
        int inserted = libraryService.importNicknames(file);
        log.info("管理员 Excel 导入模拟昵称 inserted={}", inserted);
        return Result.success(Map.of("inserted", inserted));
    }

    @GetMapping("/nicknames/template")
    public void downloadNicknameTemplate(HttpServletResponse response) throws IOException {
        checkSuperAdmin();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = URLEncoder.encode("昵称导入模板.xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        SimulationNicknameExcelUtil.writeTemplate(response.getOutputStream());
    }

    @DeleteMapping("/nicknames/{id}")
    public Result<Void> deleteNickname(@PathVariable Long id) {
        checkSuperAdmin();
        libraryService.deleteNickname(id);
        return Result.success(null);
    }

    @PostMapping("/nicknames/batch-delete")
    public Result<Map<String, Integer>> batchDeleteNicknames(@Valid @RequestBody SimulationLibraryBatchDeleteRequest request) {
        checkSuperAdmin();
        int deleted = libraryService.batchDeleteNicknames(request.getIds());
        log.info("管理员批量删除模拟昵称 deleted={}", deleted);
        return Result.success(Map.of("deleted", deleted));
    }

    // ---------- 头像库 ----------

    @GetMapping("/avatars")
    public Result<PageResult<SimulationAvatarVO>> listAvatars(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "24") Long size) {
        checkSuperAdmin();
        return Result.success(libraryService.listAvatars(page, size));
    }

    @GetMapping("/avatars/count")
    public Result<Long> countAvatars() {
        checkSuperAdmin();
        return Result.success(libraryService.countAvatars());
    }

    @PostMapping(value = "/avatars", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Integer>> uploadAvatars(@RequestPart("files") MultipartFile[] files) {
        checkSuperAdmin();
        int saved = libraryService.uploadAvatars(files);
        log.info("管理员上传模拟头像 saved={}", saved);
        return Result.success(Map.of("saved", saved));
    }

    @DeleteMapping("/avatars/{id}")
    public Result<Void> deleteAvatar(@PathVariable Long id) {
        checkSuperAdmin();
        libraryService.deleteAvatar(id);
        return Result.success(null);
    }

    @PostMapping("/avatars/batch-delete")
    public Result<Map<String, Integer>> batchDeleteAvatars(@Valid @RequestBody SimulationLibraryBatchDeleteRequest request) {
        checkSuperAdmin();
        int deleted = libraryService.batchDeleteAvatars(request.getIds());
        log.info("管理员批量删除模拟头像 deleted={}", deleted);
        return Result.success(Map.of("deleted", deleted));
    }

    private Long checkSuperAdmin() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId == null || !permissionService.isSuperAdmin(adminId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminId;
    }
}
