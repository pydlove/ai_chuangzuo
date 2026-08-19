 package com.aichuangzuo.admin.modules.platform.controller;
 
 import com.aichuangzuo.admin.modules.platform.dto.request.PlatformSaveRequest;
 import com.aichuangzuo.admin.modules.platform.service.PlatformAdminService;
 import com.aichuangzuo.admin.modules.platform.vo.PlatformVO;
 import com.aichuangzuo.shared.result.Result;
 import io.swagger.v3.oas.annotations.tags.Tag;
 import jakarta.validation.Valid;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.web.bind.annotation.DeleteMapping;
 import org.springframework.web.bind.annotation.GetMapping;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.PostMapping;
 import org.springframework.web.bind.annotation.PutMapping;
 import org.springframework.web.bind.annotation.RequestBody;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RestController;
 
 import java.util.List;
 
 /**
  * 管理端：自媒体平台配置 CRUD。
  */
 @Tag(name = "管理端-自媒体平台配置")
 @Slf4j
 @RestController
 @RequestMapping("/api/v1/admin/platforms")
 @RequiredArgsConstructor
 public class PlatformAdminController {
 
     private final PlatformAdminService platformAdminService;
 
     @GetMapping
     public Result<List<PlatformVO>> list() {
         return Result.success(platformAdminService.list());
     }
 
     @PostMapping
     public Result<PlatformVO> save(@Valid @RequestBody PlatformSaveRequest request) {
         return Result.success(platformAdminService.save(request));
     }
 
     @PutMapping("/{id}")
     public Result<PlatformVO> update(@PathVariable Long id,
                                      @Valid @RequestBody PlatformSaveRequest request) {
         return Result.success(platformAdminService.update(id, request));
     }
 
     @DeleteMapping("/{id}")
     public Result<Void> delete(@PathVariable Long id) {
         platformAdminService.delete(id);
         return Result.success();
     }
 }
