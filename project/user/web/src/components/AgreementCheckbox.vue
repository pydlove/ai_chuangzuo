<template>
  <div class="agreement-row" :class="{ shaking: isShaking }">
    <label class="agreement-label">
      <input
        type="checkbox"
        class="agreement-checkbox"
        :checked="modelValue"
        @change="$emit('update:modelValue', $event.target.checked)"
      />
      <span class="agreement-text">
        我已阅读并同意
        <span class="agreement-link" @click.stop="termsVisible = true">《用户协议》</span>
        和
        <span class="agreement-link" @click.stop="privacyVisible = true">《隐私政策》</span>
      </span>
    </label>
  </div>

  <a-modal
    v-model:open="termsVisible"
    title="用户协议"
    :footer="null"
    width="min(90vw, 720px)"
    class="legal-modal"
  >
    <div class="legal-content">
      <p>欢迎使用爱创作服务（以下简称"本服务"）。在您使用本服务之前，请仔细阅读本用户协议。</p>
      <h4>一、服务条款</h4>
      <p>您确认，在您完成注册程序或使用本服务时，您应当是具备完全民事权利能力和完全民事行为能力的自然人、法人或其他组织。</p>
      <h4>二、账号安全</h4>
      <p>您须对在本服务中注册账号下的所有行为承担法律责任。您不应将账号、密码转让或出借给他人使用。因黑客行为或您保管疏忽导致账号、密码遭他人非法使用，本平台不承担任何责任。</p>
      <h4>三、服务变更</h4>
      <p>本平台保留随时修改或中断服务而不需通知您的权利。本平台行使修改或中断服务的权利，无需对您或第三方负责。</p>
      <h4>四、协议更新</h4>
      <p>本协议内容如有更新，平台将提前通知用户，更新后的协议自公布之日起生效。</p>
    </div>
  </a-modal>

  <a-modal
    v-model:open="privacyVisible"
    title="隐私政策"
    :footer="null"
    width="min(90vw, 720px)"
    class="legal-modal"
  >
    <div class="legal-content">
      <p>我们非常重视您的个人隐私保护，在您使用爱创作服务时，我们会按照本隐私政策的规定收集、使用、存储和保护您的个人信息。</p>
      <h4>一、信息收集</h4>
      <p>我们会收集您在使用本服务过程中主动提供的个人信息，以及为提供服务所必需的设备信息、日志信息等。</p>
      <h4>二、信息使用</h4>
      <p>我们仅会在法律法规允许的范围和本政策约定的目的内使用您的个人信息，用于身份验证、服务提供、安全风控等。</p>
      <h4>三、信息保护</h4>
      <p>我们采用符合业界标准的安全防护措施保护您的个人信息，防止数据遭到未经授权的访问、公开披露、使用、修改、损坏或丢失。</p>
      <h4>四、联系我们</h4>
      <p>如您对隐私政策有任何疑问，请通过官方渠道与我们联系。</p>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  shakeCount: { type: Number, default: 0 }
})
defineEmits(['update:modelValue'])

const termsVisible = ref(false)
const privacyVisible = ref(false)
const isShaking = ref(false)

watch(() => props.shakeCount, () => {
  if (isShaking.value) return
  isShaking.value = true
  setTimeout(() => {
    isShaking.value = false
  }, 400)
})
</script>

<style scoped>
.agreement-row {
  margin: 4px 0;
}

.agreement-row.shaking {
  animation: agreement-shake 0.4s ease-in-out;
}

@keyframes agreement-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}

.agreement-label {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
  line-height: 1.5;
  color: #595959;
}

.agreement-checkbox {
  width: 16px;
  height: 16px;
  margin-top: 1px;
  flex-shrink: 0;
  accent-color: #FF2442;
  cursor: pointer;
}

.agreement-text {
  flex: 1;
}

.agreement-link {
  color: #FF2442;
  cursor: pointer;
  transition: color 0.2s;
}

.agreement-link:hover {
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

body[data-theme="dark"] .agreement-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .agreement-link {
  color: #ff4d6f;
}

body[data-theme="dark"] .agreement-link:hover {
  color: #ff7a99;
}

body[data-theme="dark"] .legal-content {
  color: #b0b0b0;
}

body[data-theme="dark"] .legal-content h4 {
  color: #e0e0e0;
}
</style>
