ALTER TABLE u_user
    ADD COLUMN coin_balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000
        COMMENT '创作币余额（正为可用）' AFTER invite_code;
