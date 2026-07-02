<template>
  <a-tooltip
    placement="top"
    :mouse-enter-delay="0.1"
    overlay-class-name="invite-coin-tooltip"
  >
    <template #title>
      <div class="invite-coin-tooltip-content">
        <div class="invite-coin-tooltip-title">💰 创作币说明</div>
        <div class="invite-coin-tooltip-desc">
          创作币是爱创作推出的虚拟货币，<b>1 创作币 = 1 元人民币</b>。
        </div>

        <div class="invite-coin-tooltip-section">
          <div class="invite-coin-tooltip-section-title">如何获得</div>
          <ul class="invite-coin-tooltip-list">
            <li>通过邀请链接注册，立刻获得 <b>5 创作币</b>（仅限新用户）</li>
            <li>邀请好友<b>首次购买</b>会员返佣 <b>10%</b></li>
            <li>邀请好友<b>续费</b>会员返佣 <b>5%</b></li>
          </ul>
        </div>

        <div class="invite-coin-tooltip-section">
          <div class="invite-coin-tooltip-section-title">可用于</div>
          <ul class="invite-coin-tooltip-list">
            <li>满 100 创作币可申请<b>提现至支付宝</b></li>
            <li>后续可用于<b>抵扣会员订阅</b>费用</li>
          </ul>
        </div>
      </div>
    </template>
    <slot />
  </a-tooltip>
</template>
