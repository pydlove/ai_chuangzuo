package com.aichuangzuo.user.modules.auth.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_qr_login_session")
public class QrLoginSession extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String qrCode;

    private Integer status;

    private Long scannerUserId;

    private String scannerNickname;

    private String clientIp;

    private String userAgent;

    private LocalDateTime expiredAt;
}
