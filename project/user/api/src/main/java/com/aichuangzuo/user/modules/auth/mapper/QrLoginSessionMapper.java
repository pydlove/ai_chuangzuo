package com.aichuangzuo.user.modules.auth.mapper;

import com.aichuangzuo.user.modules.auth.entity.QrLoginSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface QrLoginSessionMapper extends BaseMapper<QrLoginSession> {

    @Update("UPDATE u_qr_login_session SET status = #{status}, updated_at = NOW(3) WHERE qr_code = #{qrCode} AND status = #{expectedStatus} AND is_deleted = 0")
    int updateStatus(@Param("qrCode") String qrCode, @Param("status") Integer status, @Param("expectedStatus") Integer expectedStatus);
}
