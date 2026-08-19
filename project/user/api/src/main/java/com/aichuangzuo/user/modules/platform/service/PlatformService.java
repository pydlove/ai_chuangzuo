 package com.aichuangzuo.user.modules.platform.service;
 
 import com.aichuangzuo.user.modules.platform.vo.PlatformVO;
 
 import java.util.List;
 
 /**
  * 自媒体平台配置用户端服务。
  */
 public interface PlatformService {
 
     List<PlatformVO> listActivePlatforms();
 }
