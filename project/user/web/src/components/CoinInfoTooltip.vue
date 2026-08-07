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
          创作币是爱创作推出的虚拟货币，<b>10 创作币 = 1 元人民币</b>。
        </div>

        <div class="invite-coin-tooltip-section">
          <div class="invite-coin-tooltip-section-title">如何获得</div>
          <ul class="invite-coin-tooltip-list">
            <li>通过邀请链接注册，受邀请的新用户在绑定邀请人之后，立刻获得 <b>50 创作币</b></li>
            <li>邀请好友注册，好友<b>首次购买</b>返佣 <b>10%</b>，后续<b>续费</b>返佣 <b>5%</b>，只要邀请用户在平台消费即可获得佣金</li>
            <li>参与约稿中心的活动，<b>中稿</b>之后获得创作币奖励</li>
            <li>发布提示词到提示词市场，提示词被其他用户用来生成文章，可获得 <b>2 创作币 / 次</b></li>
            <li>收益进入收益排行榜 <b>TOP3</b>，可获得 <b>500 创作币</b>奖励（相当于 50 人民币，每月一次）</li>
          </ul>
        </div>

        <div class="invite-coin-tooltip-section">
          <div class="invite-coin-tooltip-section-title">可用于</div>
          <ul class="invite-coin-tooltip-list">
            <li>满 1000 创作币可申请<b>提现至支付宝</b></li>
            <li>后续可用于<b>抵扣会员订阅</b>费用</li>
          </ul>
        </div>
      </div>
    </template>
    <slot />
  </a-tooltip>
</template>
