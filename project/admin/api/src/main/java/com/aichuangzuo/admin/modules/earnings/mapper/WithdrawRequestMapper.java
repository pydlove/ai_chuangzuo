package com.aichuangzuo.admin.modules.earnings.mapper;

import com.aichuangzuo.admin.modules.earnings.entity.WithdrawRequest;
import com.aichuangzuo.admin.modules.earnings.vo.WithdrawAdminVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WithdrawRequestMapper extends BaseMapper<WithdrawRequest> {

    List<WithdrawAdminVO> selectWithdrawAdminPage(@Param("userId") Long userId,
                                                   @Param("bizNo") String bizNo,
                                                   @Param("status") Integer status,
                                                   @Param("offset") long offset,
                                                   @Param("size") long size);

    long countWithdrawAdminPage(@Param("userId") Long userId,
                                @Param("bizNo") String bizNo,
                                @Param("status") Integer status);
}
