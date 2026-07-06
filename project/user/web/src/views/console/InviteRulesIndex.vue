<template>
  <div class="invite-rules-page">
    <h2 class="invite-rules-page-title">活动规则详情</h2>

    <section class="invite-rules-section">
      <h4 class="invite-rules-heading">邀请返佣机制</h4>
      <div class="invite-rules-callout">
        推荐新客下单即获得奖励，<b>一次邀请终身享受订单返佣红利</b>。
      </div>
      <ul class="invite-rules-list">
        <li>通过专属邀请链接或邀请码建立邀请关系，好友注册成功即生效。</li>
        <li>累计邀请 3 人奖励 3 天专业版会员；累计邀请 5 人奖励 5 天专业版会员。</li>
        <li>超过 5 人后，每多邀请 1 人额外奖励 2 天专业版会员，会员天数可累计叠加。</li>
        <li>好友首次购买会员可获 10% 创作币返佣，续费返佣 5%，返佣以创作币形式即时到账。</li>
        <li>被邀请的好友通过你的链接首次下单后，该笔订单视为你邀请的返佣订单。</li>
      </ul>
    </section>

    <section class="invite-rules-section">
      <h4 class="invite-rules-heading">提现规则</h4>
      <ul class="invite-rules-list">
        <li>仅付费邀请用户可申请提现，且需至少邀请 3 位付费好友。</li>
        <li>创作币余额满 100 即可提现，1 创作币 = 1 元人民币。</li>
        <li>提现申请提交后约 1 个工作日审核，预计 7 个工作日内到账。</li>
        <li>目前仅支持支付宝提现，请确保支付宝账号和真实姓名与本人一致。</li>
        <li>因账号信息错误、账户异常等原因导致的提现失败，平台概不负责。</li>
      </ul>
    </section>

    <section class="invite-rules-section">
      <h4 class="invite-rules-heading">声明</h4>
      <ul class="invite-rules-list">
        <li>根据业务发展和市场环境变化，活动规则可能调整，会通过官方渠道提前通知。</li>
        <li>平台保留对违规刷量、虚假邀请、机器注册等行为的处理权。</li>
        <li>本活动最终解释权归爱创作所有，如有疑问请联系客服。</li>
      </ul>
    </section>
  </div>
</template>

<script setup>
</script>

<style scoped>
.invite-rules-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 20px 16px 32px;
}

.invite-rules-page-title {
  margin: 0 0 24px;
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}

.invite-rules-section {
  background: #fff;
  border-radius: 14px;
  padding: 18px 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.invite-rules-heading {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.invite-rules-callout {
  padding: 12px 14px;
  background: #fff5f7;
  border-left: 3px solid #ff2442;
  border-radius: 8px;
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
  margin-bottom: 12px;
}

.invite-rules-callout b {
  color: #ff2442;
}

.invite-rules-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #595959;
  line-height: 1.8;
}

.invite-rules-list li {
  margin-bottom: 6px;
}

.invite-rules-list li:last-child {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .invite-rules-page {
    padding: 16px 12px 24px;
  }

  .invite-rules-page-title {
    font-size: 20px;
    margin-bottom: 16px;
  }
}

/* 暗色主题 */
body[data-theme="dark"] .invite-rules-page-title,
body[data-theme="dark"] .invite-rules-heading {
  color: #f0f0f0;
}

body[data-theme="dark"] .invite-rules-section {
  background: #1f1f1f;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .invite-rules-list,
body[data-theme="dark"] .invite-rules-callout {
  color: #a6a6a6;
}

body[data-theme="dark"] .invite-rules-callout {
  background: rgba(255, 36, 66, 0.12);
}
</style>