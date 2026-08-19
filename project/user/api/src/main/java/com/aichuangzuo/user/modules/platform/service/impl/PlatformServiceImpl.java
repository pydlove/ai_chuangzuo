 package com.aichuangzuo.user.modules.platform.service.impl;
 
 import com.aichuangzuo.shared.entity.Platform;
 import com.aichuangzuo.user.modules.platform.mapper.PlatformMapper;
 import com.aichuangzuo.user.modules.platform.service.PlatformService;
 import com.aichuangzuo.user.modules.platform.vo.PlatformVO;
 import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.stereotype.Service;
 
 import java.util.List;
 import java.util.stream.Collectors;
 
 /**
  * 自媒体平台配置用户端服务实现。
  */
 @Slf4j
 @Service
 @RequiredArgsConstructor
 public class PlatformServiceImpl implements PlatformService {
 
     private final PlatformMapper platformMapper;
     private final ObjectMapper objectMapper;
 
     @Override
     public List<PlatformVO> listActivePlatforms() {
         List<Platform> platforms = platformMapper.selectList(new LambdaQueryWrapper<Platform>()
                 .eq(Platform::getStatus, 1)
                 .orderByAsc(Platform::getSortOrder)
                 .orderByAsc(Platform::getId));
         log.debug("查询用户端平台列表, size={}", platforms.size());
         return platforms.stream()
                 .map(p -> PlatformVO.from(p, objectMapper))
                 .collect(Collectors.toList());
     }
 }
