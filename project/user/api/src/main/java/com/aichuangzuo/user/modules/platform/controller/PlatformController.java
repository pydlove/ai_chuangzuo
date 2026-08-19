 package com.aichuangzuo.user.modules.platform.controller;
 
 import com.aichuangzuo.shared.result.Result;
 import com.aichuangzuo.user.modules.platform.service.PlatformService;
 import com.aichuangzuo.user.modules.platform.vo.PlatformVO;
 import io.swagger.v3.oas.annotations.tags.Tag;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.web.bind.annotation.GetMapping;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RestController;
 
 import java.util.List;
 
 /**
  * 用户端：自媒体平台配置查询（制定自媒体方案第一步）。
  */
 @Tag(name = "用户端-自媒体平台配置")
 @Slf4j
 @RestController
 @RequestMapping("/api/v1/user/platforms")
 @RequiredArgsConstructor
 public class PlatformController {
 
     private final PlatformService platformService;
 
     @GetMapping
     public Result<List<PlatformVO>> listActivePlatforms() {
         return Result.success(platformService.listActivePlatforms());
     }
 }
