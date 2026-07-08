package com.aichuangzuo.user.modules.earnings.mapper;

import com.aichuangzuo.user.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.user.modules.earnings.vo.MonthlySettlementVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户收益记录 Mapper。
 */
@Mapper
public interface EarningsRecordMapper extends BaseMapper<EarningsRecord> {

    /**
     * 按月聚合用户收益记录。
     *
     * @param userId 用户ID
     * @return 月度结算列表
     */
    List<MonthlySettlementVO> selectMonthlySettlementList(@Param("userId") Long userId);
}
