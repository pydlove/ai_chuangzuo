-- 修复管理端显示的历史/测试邀请码长度不规范的问题。
-- 把 u_user 中长度不是 6 位或为空 invite_code 重新生成为 6 位唯一码，
-- 使管理端复制的邀请码都能直接用于注册。

DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS normalize_user_invite_codes()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE user_id BIGINT UNSIGNED;
    DECLARE candidate VARCHAR(16);
    DECLARE existing_count INT;
    DECLARE chars VARCHAR(32) DEFAULT 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

    -- 仅处理未被软删且 invite_code 长度不是 6 位的用户
    DECLARE cur CURSOR FOR
        SELECT id FROM u_user
        WHERE is_deleted = 0 AND (invite_code IS NULL OR LENGTH(invite_code) != 6);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO user_id;
        IF done THEN
            LEAVE read_loop;
        END IF;

        gen_loop: LOOP
            SET candidate = CONCAT(
                SUBSTRING(chars, FLOOR(1 + RAND() * 32), 1),
                SUBSTRING(chars, FLOOR(1 + RAND() * 32), 1),
                SUBSTRING(chars, FLOOR(1 + RAND() * 32), 1),
                SUBSTRING(chars, FLOOR(1 + RAND() * 32), 1),
                SUBSTRING(chars, FLOOR(1 + RAND() * 32), 1),
                SUBSTRING(chars, FLOOR(1 + RAND() * 32), 1)
            );
            SELECT COUNT(*) INTO existing_count FROM u_user WHERE invite_code = candidate;
            IF existing_count = 0 THEN
                LEAVE gen_loop;
            END IF;
        END LOOP;

        UPDATE u_user SET invite_code = candidate WHERE id = user_id;
    END LOOP;
    CLOSE cur;
END$$

DELIMITER ;

CALL normalize_user_invite_codes();
DROP PROCEDURE normalize_user_invite_codes;
