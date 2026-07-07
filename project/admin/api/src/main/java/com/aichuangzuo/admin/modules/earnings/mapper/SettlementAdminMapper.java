package com.aichuangzuo.admin.modules.earnings.mapper;

import com.aichuangzuo.admin.modules.earnings.vo.PendingSettlementUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SettlementAdminMapper {

    List<PendingSettlementUserVO> selectPendingUsers(@Param("month") String month);

    long countPendingUsers(@Param("month") String month);

    List<PendingSettlementUserVO> selectPendingAmountBeforeSettle(
            @Param("month") String month,
            @Param("userIds") List<Long> userIds);
}
