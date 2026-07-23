package com.aichuangzuo.user.modules.user.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateNicknameRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldAccept20CharsNickname() {
        UpdateNicknameRequest req = new UpdateNicknameRequest();
        req.setNickname("12345678901234567890");
        Set<ConstraintViolation<UpdateNicknameRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "20 字符昵称应通过校验");
    }

    @Test
    void shouldReject21CharsNickname() {
        UpdateNicknameRequest req = new UpdateNicknameRequest();
        req.setNickname("123456789012345678901");
        Set<ConstraintViolation<UpdateNicknameRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "21 字符昵称应被拦截");
    }

    @Test
    void shouldRejectBlankNickname() {
        UpdateNicknameRequest req = new UpdateNicknameRequest();
        req.setNickname("   ");
        Set<ConstraintViolation<UpdateNicknameRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "空白昵称应被拦截");
    }
}
