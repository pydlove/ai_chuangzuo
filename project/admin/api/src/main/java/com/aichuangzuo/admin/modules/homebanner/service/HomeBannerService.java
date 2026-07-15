package com.aichuangzuo.admin.modules.homebanner.service;

import com.aichuangzuo.admin.modules.homebanner.dto.request.HomeBannerReq;
import com.aichuangzuo.admin.modules.homebanner.vo.HomeBannerVO;

import java.util.List;

public interface HomeBannerService {

    List<HomeBannerVO> list();

    Long create(HomeBannerReq req);

    void update(Long id, HomeBannerReq req);

    void delete(Long id);
}
