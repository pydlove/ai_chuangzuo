package com.aichuangzuo.admin.modules.earnings.mapper;

import com.aichuangzuo.admin.modules.earnings.vo.UserAccountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccountAdminMapper {

    List<UserAccountVO> selectAccountList(@Param("userId") Long userId,
                                          @Param("nickname") String nickname,
                                          @Param("phone") String phone,
                                          @Param("email") String email,
                                          @Param("offset") long offset,
                                          @Param("size") long size);

    long countAccountList(@Param("userId") Long userId,
                          @Param("nickname") String nickname,
                          @Param("phone") String phone,
                          @Param("email") String email);

    Integer selectCoinRank(@Param("userId") Long userId,
                           @Param("month") String month);

    Integer selectIncomeRank(@Param("userId") Long userId,
                             @Param("month") String month);
}
