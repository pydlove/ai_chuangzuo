package com.aichuangzuo.user.modules.membership.mapper;

import com.aichuangzuo.user.modules.membership.entity.Plan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 套餐元数据 Mapper。
 */
@Mapper
public interface PlanMapper extends BaseMapper<Plan> {

    /**
     * 定价目录缓存版本号：u_plan / u_benefit / u_plan_benefit 三张表最近一次更新时间。
     * 管理端改动任一表后版本变化，用户端缓存 key 随之变化，实现跨进程即时失效。
     */
    @Select("SELECT MAX(ts) FROM ("
            + " SELECT MAX(updated_at) AS ts FROM u_plan"
            + " UNION ALL SELECT MAX(updated_at) FROM u_benefit"
            + " UNION ALL SELECT MAX(updated_at) FROM u_plan_benefit"
            + ") t")
    LocalDateTime selectCatalogVersion();
}