package com.aichuangzuo.admin.modules.selfmediaplan.service;

import com.aichuangzuo.admin.modules.selfmediaplan.dto.request.SelfMediaPlanPageRequest;
import com.aichuangzuo.admin.modules.selfmediaplan.vo.SelfMediaPlanVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface SelfMediaPlanAdminService {

    IPage<SelfMediaPlanVO> page(SelfMediaPlanPageRequest request);
}
