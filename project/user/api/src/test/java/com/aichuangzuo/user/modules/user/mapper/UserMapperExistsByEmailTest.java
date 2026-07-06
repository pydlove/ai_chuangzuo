package com.aichuangzuo.user.modules.user.mapper;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class UserMapperExistsByEmailTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldReturnTrueWhenEmailExistsForAnotherUser() {
        User u1 = newUser("exists-a@test.com");
        User u2 = newUser("exists-b@test.com");
        userMapper.insert(u1);
        userMapper.insert(u2);
        // u1 视角看 u2 的邮箱：应该存在
        assertTrue(userMapper.existsByEmail(u2.getEmail(), u1.getId()));
    }

    @Test
    void shouldReturnFalseWhenEmailBelongsToExcludedUser() {
        User u1 = newUser("self@test.com");
        userMapper.insert(u1);
        // 排除自己后，自己邮箱不再算"冲突"
        assertFalse(userMapper.existsByEmail(u1.getEmail(), u1.getId()));
    }

    @Test
    void shouldReturnFalseWhenEmailNotRegistered() {
        assertFalse(userMapper.existsByEmail("nobody@test.com", 0L));
    }

    private User newUser(String email) {
        User u = new User();
        u.setBizNo("B" + System.nanoTime());
        u.setNickname("u");
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setInviteCode("I" + System.nanoTime() % 1000000);
        u.setUserStatus(1);
        u.setEmailVerified(0);
        return u;
    }
}