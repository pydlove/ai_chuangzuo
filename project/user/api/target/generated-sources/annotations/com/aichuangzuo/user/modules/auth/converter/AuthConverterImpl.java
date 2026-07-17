package com.aichuangzuo.user.modules.auth.converter;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.vo.UserVO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-17T09:35:45+0800",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class AuthConverterImpl implements AuthConverter {

    @Override
    public UserVO toUserVO(User user) {
        if ( user == null ) {
            return null;
        }

        UserVO userVO = new UserVO();

        userVO.setId( user.getId() );
        userVO.setBizNo( desensitizeEmail( user.getBizNo() ) );
        userVO.setNickname( desensitizeEmail( user.getNickname() ) );
        userVO.setAvatarUrl( desensitizeEmail( user.getAvatarUrl() ) );

        userVO.setEmail( desensitizeEmail(user.getEmail()) );

        return userVO;
    }
}
