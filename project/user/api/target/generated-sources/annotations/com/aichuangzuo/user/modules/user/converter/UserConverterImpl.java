package com.aichuangzuo.user.modules.user.converter;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-24T17:08:22+0800",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class UserConverterImpl implements UserConverter {

    @Override
    public UserProfileVO toProfileVO(User user) {
        if ( user == null ) {
            return null;
        }

        UserProfileVO userProfileVO = new UserProfileVO();

        userProfileVO.setUserId( user.getBizNo() );
        userProfileVO.setNickname( user.getNickname() );
        userProfileVO.setEmail( user.getEmail() );
        userProfileVO.setAvatarUrl( user.getAvatarUrl() );
        userProfileVO.setEmailVerified( user.getEmailVerified() );
        userProfileVO.setInviteCode( user.getInviteCode() );

        return userProfileVO;
    }
}
