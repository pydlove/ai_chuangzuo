package com.aichuangzuo.admin.modules.learn.service;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnBannerReq;
import com.aichuangzuo.admin.modules.learn.vo.LearnBannerVO;

import java.util.List;

public interface LearnBannerService {

    List<LearnBannerVO> list();

    Long create(LearnBannerReq req);

    void update(Long id, LearnBannerReq req);

    void delete(Long id);
}
