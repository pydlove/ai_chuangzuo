<template>
  <span class="paid-agreement-text">
    支付即表示你已阅读并同意
    <span class="paid-agreement-link" @click.stop="visible = true">爱创作工坊 付费服务协议</span>
  </span>

  <a-modal
    v-model:open="visible"
    title="爱创作工坊 付费服务协议"
    :footer="null"
    width="min(90vw, 720px)"
    class="legal-modal paid-agreement-modal"
  >
    <div class="legal-content">
      <p>欢迎使用爱创作工坊（以下简称"本平台"）提供的付费会员服务（以下简称"本服务"）。本协议由您与本平台共同缔结，对双方具有约束力。在您购买或使用本服务之前，请认真阅读并充分理解本协议全部内容。</p>

      <h4>一、服务说明</h4>
      <p>1.1 本服务为本平台提供的会员订阅服务，包括但不限于基础版、专业版、旗舰版等不同等级的会员权益。具体权益内容以购买页面公示及本平台不时更新为准。</p>
      <p>1.2 本平台有权基于运营需要调整会员权益、服务价格及服务规则，调整内容将在相关页面公布，公布后即视为已送达用户。</p>

      <h4>二、购买与支付</h4>
      <p>2.1 您应使用本平台支持的支付方式进行购买。支付完成后，系统将自动为您开通相应会员权益。</p>
      <p>2.2 会员服务属于虚拟商品，具有即时生效、不可退回的特性。一经支付成功，不支持退款、转让或兑换现金，请您在购买前仔细确认所选套餐、周期及金额。</p>
      <p>2.3 如因系统故障、网络异常或其他非用户原因导致充值未到账，您可联系本平台客服，经核实后本平台将予以补足或协商处理。</p>

      <h4>三、会员有效期与续费</h4>
      <p>3.1 会员有效期自支付成功之日起计算，具体以购买页面展示及系统记录为准。</p>
      <p>3.2 会员到期日前 7 天，本平台将通过消息中心向您发送到期提醒，提醒您及时续费。该提醒仅作为便利性服务，不构成本平台对续费成功的保证。</p>
      <p>3.3 会员到期后，您将不再享有对应会员权益，本平台不因此承担任何责任。</p>

      <h4>四、用户行为规范</h4>
      <p>4.1 您应妥善保管账号及密码，不得将会员账号转让、出借、共享或出售给他人。因您保管不善导致的损失，由您自行承担。</p>
      <p>4.2 您不得利用本服务从事任何违法违规、侵犯第三方合法权益或扰乱平台秩序的行为。否则，本平台有权暂停或终止您的会员权益，并保留追究法律责任的权利。</p>

      <h4>五、未成年人保护</h4>
      <p>5.1 未成年用户应在监护人陪同下理性充值，避免过度消费。监护人应对未成年人的充值行为进行监督和指导。</p>
      <p>5.2 如您认为未成年人未经监护人同意进行了大额充值，请立即联系本平台客服，本平台将在法律法规允许范围内协助处理。</p>

      <h4>六、免责声明</h4>
      <p>6.1 本平台将尽力保障服务的稳定性与连续性，但因不可抗力、第三方原因或系统维护等因素导致的服务中断、延迟或数据丢失，本平台不承担责任。</p>
      <p>6.2 您理解并同意，本服务项下由第三方提供的内容、链接或服务，本平台仅提供信息展示，不对其真实性、合法性、准确性负责。</p>

      <h4>七、协议变更与终止</h4>
      <p>7.1 本平台有权根据法律法规变化及业务发展需要，对本协议进行修改。修改后的协议将在本平台显著位置公布，公布后即视为已送达用户。</p>
      <p>7.2 如您不同意修改后的协议，应立即停止使用本服务。您继续使用本服务的，视为同意修改后的协议。</p>

      <h4>八、其他</h4>
      <p>8.1 本协议的订立、执行、解释及争议解决均适用中华人民共和国法律。</p>
      <p>8.2 如您对本协议或本服务有任何疑问，可通过本平台客服渠道与我们联系。</p>
      <p>8.3 本协议自您点击"微信支付"或完成支付之时起生效。</p>
    </div>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'

const visible = ref(false)
</script>

<style scoped>
.paid-agreement-text {
  font-size: 14px;
  line-height: 1.5;
  color: #8c8c8c;
}

.paid-agreement-link {
  color: #FF2442;
  cursor: pointer;
  transition: color 0.2s;
}

.paid-agreement-link:hover {
  color: #E61E3A;
}

.legal-content {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 8px;
  font-size: 14px;
  line-height: 1.8;
  color: #434343;
}

.legal-content h4 {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 16px 0 8px;
}

.legal-content p {
  margin: 0 0 12px;
}

body[data-theme="dark"] .paid-agreement-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .paid-agreement-link {
  color: #ff4d6f;
}

body[data-theme="dark"] .paid-agreement-link:hover {
  color: #ff7a99;
}

body[data-theme="dark"] .legal-content {
  color: #b0b0b0;
}

body[data-theme="dark"] .legal-content h4 {
  color: #e0e0e0;
}
</style>
