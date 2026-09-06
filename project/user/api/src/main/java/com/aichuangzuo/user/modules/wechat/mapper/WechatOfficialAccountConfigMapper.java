package com.aichuangzuo.user.modules.wechat.mapper;

import com.aichuangzuo.shared.entity.WechatOfficialAccountConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 微信公众号配置 Mapper（读取 admin 端维护的配置）。
 */
@Mapper
public interface WechatOfficialAccountConfigMapper extends BaseMapper<WechatOfficialAccountConfig> {
}
