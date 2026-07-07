package com.aichuangzuo.user.modules.hotsearch.mapper;

import com.aichuangzuo.user.modules.hotsearch.entity.HotSearchDaily;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HotSearchDailyMapper extends BaseMapper<HotSearchDaily> {

    /**
     * 按平台和日期查询榜单，按排名升序。
     */
    List<HotSearchDaily> selectByPlatformAndDate(@Param("platformCode") String platformCode,
                                                  @Param("snapshotDate") LocalDate snapshotDate);
}
