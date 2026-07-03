package com.aichuangzuo.user.modules.auth.mapper;

import com.aichuangzuo.user.modules.auth.entity.IpRegisterLimit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IpRegisterLimitMapper extends BaseMapper<IpRegisterLimit> {

    @Update("INSERT INTO u_ip_register_limit (client_ip, register_count, is_blocked) " +
            "VALUES (#{clientIp}, 1, 0) " +
            "ON DUPLICATE KEY UPDATE register_count = register_count + 1")
    int incrementRegisterCount(@Param("clientIp") String clientIp);
}
