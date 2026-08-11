ALTER TABLE u_lottery_campaign
    ADD COLUMN image_url VARCHAR(512) DEFAULT NULL COMMENT '宣传图URL' AFTER description,
    ADD COLUMN rules TEXT DEFAULT NULL COMMENT '活动规则（可填写奖项说明等）' AFTER image_url;
