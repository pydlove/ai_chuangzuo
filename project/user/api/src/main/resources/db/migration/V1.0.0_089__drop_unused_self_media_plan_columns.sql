SET NAMES utf8mb4;

ALTER TABLE u_self_media_plan
    DROP COLUMN goal,
    DROP COLUMN background,
    DROP COLUMN has_product,
    DROP COLUMN product_desc;
