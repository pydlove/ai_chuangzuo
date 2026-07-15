package com.aichuangzuo.user.modules.homebanner.service;

import com.aichuangzuo.user.modules.homebanner.vo.HomeBannerVO;

import java.util.List;

public interface HomeBannerService {

    /** 首页 Banner 列表（所有未删除，按 sort ASC） */
    List<HomeBannerVO> list();
}
