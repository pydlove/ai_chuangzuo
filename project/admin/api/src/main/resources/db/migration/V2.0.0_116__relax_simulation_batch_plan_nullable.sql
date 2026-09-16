-- 模拟生成文章批次无会员套餐，plan_key/plan_name/cycle 允许为空
ALTER TABLE a_simulation_batch
    MODIFY COLUMN plan_key  VARCHAR(16) NULL COMMENT '会员套餐 key：basic/pro/flagship（模拟生成文章批次为空）',
    MODIFY COLUMN plan_name VARCHAR(64) NULL COMMENT '会员套餐名称快照（模拟生成文章批次为空）',
    MODIFY COLUMN cycle     VARCHAR(16) NULL COMMENT '订阅周期：month/quarter/year（模拟生成文章批次为空）';
