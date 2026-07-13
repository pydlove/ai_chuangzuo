SET NAMES utf8mb4;

-- 发布=唯一生效：历史数据收敛 + 删除 enabled 双轨字段
-- 背景：t_prompt_template 曾同时存在 template_status 与 enabled 两套生效语义，
-- 且从未强制唯一，线上可能有多条 PUBLISHED。收敛规则：保留最小 id 的已发布模板，
-- 其余置为已下线（template_status=2），其已发布版本行同步置为已下线。

-- 1. 保留最小 id 的已发布模板，其余已发布模板置为已下线
UPDATE t_prompt_template
SET template_status = 2
WHERE template_status = 1
  AND is_deleted = 0
  AND id != (SELECT min_id
             FROM (SELECT MIN(id) AS min_id
                   FROM t_prompt_template
                   WHERE template_status = 1
                     AND is_deleted = 0) tmp);

-- 2. 被下线模板的已发布版本行同步置为已下线
UPDATE t_prompt_template_version v
         JOIN t_prompt_template t ON t.id = v.template_id
SET v.version_status = 2
WHERE t.template_status = 2
  AND v.version_status = 1
  AND v.is_deleted = 0;

-- 3. 删除 enabled 双轨字段（生效语义只保留 template_status）
ALTER TABLE t_prompt_template
    DROP COLUMN enabled;
