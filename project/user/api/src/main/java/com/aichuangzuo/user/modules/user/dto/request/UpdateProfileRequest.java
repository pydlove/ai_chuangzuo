package com.aichuangzuo.user.modules.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 修改个人资料请求。
 */
@Data
public class UpdateProfileRequest {

    /** 昵称；1-20 字符。 */
    @Size(min = 1, max = 20, message = "昵称长度必须在 1-20 个字符之间")
    private String nickname;

    /** 个人简介；最大 256 字符。 */
    @Size(max = 256, message = "个人简介不能超过 256 个字符")
    private String bio;

    /** 性别：0-保密，1-男，2-女。 */
    private Integer gender;

    /** 生日。 */
    private LocalDate birthday;

    /** 所在地；最大 128 字符。 */
    @Size(max = 128, message = "所在地不能超过 128 个字符")
    private String location;

    /** 职业；最大 128 字符。 */
    @Size(max = 128, message = "职业不能超过 128 个字符")
    private String occupation;
}
