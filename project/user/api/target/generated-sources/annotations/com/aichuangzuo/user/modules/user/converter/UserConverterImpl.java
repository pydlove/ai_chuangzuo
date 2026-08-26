package com.aichuangzuo.user.modules.user.converter;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import java.time.format.DateTimeFormatter;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-26T13:38:54+0800",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class UserConverterImpl implements UserConverter {

    private final DateTimeFormatter dateTimeFormatter_yyyy_MM_dd_0159776256 = DateTimeFormatter.ofPattern( "yyyy-MM-dd" );

    @Override
    public UserProfileVO toProfileVO(User user) {
        if ( user == null ) {
            return null;
        }

        UserProfileVO userProfileVO = new UserProfileVO();

        userProfileVO.setUserId( user.getBizNo() );
        if ( user.getBirthday() != null ) {
            userProfileVO.setBirthday( dateTimeFormatter_yyyy_MM_dd_0159776256.format( user.getBirthday() ) );
        }
        userProfileVO.setNickname( user.getNickname() );
        userProfileVO.setEmail( user.getEmail() );
        userProfileVO.setEmailVerified( user.getEmailVerified() );
        userProfileVO.setPhone( user.getPhone() );
        userProfileVO.setPhoneVerified( user.getPhoneVerified() );
        userProfileVO.setAvatarUrl( user.getAvatarUrl() );
        userProfileVO.setBio( user.getBio() );
        userProfileVO.setGender( user.getGender() );
        userProfileVO.setLocation( user.getLocation() );
        userProfileVO.setInviteCode( user.getInviteCode() );

        return userProfileVO;
    }
}
