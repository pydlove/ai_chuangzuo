package com.aichuangzuo.user.modules.hotsearch.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HotSearchItemVO {

    private Integer rank;

    private String title;

    private String hotValue;

    private String url;

    private Long searchCount;
}
