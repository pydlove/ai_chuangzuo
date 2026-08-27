ALTER TABLE u_feedback
    ADD COLUMN star_rating TINYINT UNSIGNED NULL COMMENT '评价星级 1-5' AFTER content;
