package com.aichuangzuo.admin.modules.settings.upgrademanagement.mapper;

import com.aichuangzuo.admin.modules.settings.upgrademanagement.entity.UpgradeJobLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 升级脚本执行日志 Mapper。
 */
@Mapper
public interface UpgradeJobLogMapper extends BaseMapper<UpgradeJobLog> {
}
