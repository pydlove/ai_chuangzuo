 package com.aichuangzuo.admin.modules.platform.service;
 
 import com.aichuangzuo.admin.modules.platform.dto.request.PlatformSaveRequest;
 import com.aichuangzuo.admin.modules.platform.vo.PlatformVO;
 
 import java.util.List;
 
 /**
  * 自媒体平台配置管理端服务。
  */
 public interface PlatformAdminService {
 
     List<PlatformVO> list();
 
     PlatformVO save(PlatformSaveRequest request);
 
     PlatformVO update(Long id, PlatformSaveRequest request);
 
     void delete(Long id);
 }
