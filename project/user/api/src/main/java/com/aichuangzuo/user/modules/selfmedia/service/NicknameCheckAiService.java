package com.aichuangzuo.user.modules.selfmedia.service;

import com.aichuangzuo.user.modules.selfmedia.vo.NicknameCheckVO;
import com.aichuangzuo.user.modules.selfmedia.vo.NicknameRecommendVO;

public interface NicknameCheckAiService {

    /**
     * 基于用户自媒体定位检测昵称是否契合，不契合时给出建议昵称。
     *
     * @param platform    平台显示名
     * @param positioning 自媒体定位摘要
     * @param nickname    待检测昵称
     * @return 检测结果
     */
    NicknameCheckVO checkNickname(String platform, String positioning, String nickname);

    /**
     * 基于用户自媒体定位推荐账号昵称和简介。
     *
     * @param platform    平台显示名
     * @param positioning 自媒体定位摘要
     * @return 推荐结果
     */
    NicknameRecommendVO recommendNickname(String platform, String positioning);
}

