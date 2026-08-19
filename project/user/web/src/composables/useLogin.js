
import { ref, reactive, watch, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getRefFromUrl } from '@/composables/useInviteCode'
import { sendEmailCode, sendSmsCode, register as registerApi, login as loginApi } from '@/api/auth'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE_REGEX = /^1[3-9]\d{9}$/

// 用户作用域的本地缓存：换账号时一并清空
const USER_ID_KEY = 'aichuangzuo_user_id'
const USER_SCOPED_KEYS = [
  'aichuangzuo_user_id',
  'aichuangzuo_membership',
  'aichuangzuo_newcomer_modal_dismissed',
  'aichuangzuo_newcomer_banner_dismissed',
  'aichuangzuo_invite_modal_dismissed',
  'aichuangzuo_current_article',
  'aichuangzuo_drafts',
  'aichuangzuo_create_form',
  'aichuangzuo_create_mode',
  'aichuangzuo_create_last_skill',
  'aichuangzuo_earnings_records',
  'aichuangzuo_coin_balance',
  'aichuangzuo_redeem_codes',
  'aichuangzuo_redeem_history',
  'aichuangzuo_withdraw_agreement_accepted'
]

export function useLogin() {
  const router = useRouter()

  const activeTab = ref('login')
  const showInviteBanner = ref(false)
  const agreed = ref(false)
  const agreementShakeCount = ref(0)

  const loginMode = ref('email')
  const registerMode = ref('email')

  const loginForm = reactive({
    email: '',
    phone: '',
    password: ''
  })

  const registerForm = reactive({
    email: '',
    phone: '',
    code: '',
    password: '',
    confirmPassword: '',
    inviteCode: ''
  })

  const loginIdentifier = computed(() => {
    if (loginMode.value === 'email') {
      return loginForm.email
    }
    return loginForm.phone
  })

  // 注册流程：发送验证码前的滑块弹框
  const sliderModalVisible = ref(false)
  const sliderModalPassed = ref(false)
  let modalSending = false

  // 登录流程：调用后端登录接口前的滑块弹框
  const loginSliderModalVisible = ref(false)
  const loginModalPassed = ref(false)
  let loginModalSending = false

  const rememberMe = ref(localStorage.getItem('aichuangzuo_remember_me') === 'true')

  watch(rememberMe, (val) => {
    localStorage.setItem('aichuangzuo_remember_me', val ? 'true' : 'false')
  })

  // 注册弹框内滑块通过 → 发送验证码
  watch(sliderModalPassed, async (val) => {
    if (!val || modalSending) return
    modalSending = true
    try {
      if (registerMode.value === 'email') {
        await sendEmailCode({ email: registerForm.email })
      } else {
        await sendSmsCode({ phone: registerForm.phone })
      }
      startCodeCountdown()
      message.success('验证码已发送')
    } catch (err) {
      message.error(err?.message || '发送失败')
    } finally {
      sliderModalVisible.value = false
      modalSending = false
    }
  })

  // 登录弹框内滑块通过 → 调后端登录接口
  watch(loginModalPassed, async (val) => {
    if (!val || loginModalSending) return
    loginModalSending = true
    try {
      const payload = { password: loginForm.password, rememberMe: rememberMe.value }
      if (loginMode.value === 'email') {
        payload.email = loginForm.email
      } else {
        payload.phone = loginForm.phone
      }
      const res = await loginApi(payload)
      persistTokens(res.data)
      message.success('登录成功')
      loginSliderModalVisible.value = false
      const redirect = router.currentRoute.value.query.redirect
      router.push(typeof redirect === 'string' && redirect ? decodeURIComponent(redirect) : '/console')
    } catch (err) {
      message.error(err?.message || '登录失败')
      loginSliderModalVisible.value = false
    } finally {
      loginModalSending = false
    }
  })

  const openSliderModal = () => {
    if (codeCountdown.value > 0) return
    if (registerMode.value === 'email') {
      if (!registerForm.email) {
        message.warning('请先填写邮箱')
        return
      }
      if (!EMAIL_REGEX.test(registerForm.email)) {
        message.warning('邮箱格式不正确')
        return
      }
    } else {
      if (!registerForm.phone) {
        message.warning('请先填写手机号')
        return
      }
      if (!PHONE_REGEX.test(registerForm.phone)) {
        message.warning('手机号格式不正确')
        return
      }
    }
    sliderModalPassed.value = false
    sliderModalVisible.value = true
  }

  const openLoginSliderModal = () => {
    if (loginMode.value === 'email') {
      if (!loginForm.email) {
        message.warning('请填写邮箱')
        return
      }
      if (!EMAIL_REGEX.test(loginForm.email)) {
        message.warning('邮箱格式不正确')
        return
      }
    } else {
      if (!loginForm.phone) {
        message.warning('请填写手机号')
        return
      }
      if (!PHONE_REGEX.test(loginForm.phone)) {
        message.warning('手机号格式不正确')
        return
      }
    }
    if (!loginForm.password) {
      message.warning('请填写密码')
      return
    }
    loginModalPassed.value = false
    loginSliderModalVisible.value = true
  }

  const codeCountdown = ref(0)
  let countdownTimer = null

  const startCodeCountdown = () => {
    codeCountdown.value = 60
    if (countdownTimer) clearInterval(countdownTimer)
    countdownTimer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  }

  const persistTokens = (data) => {
    const prevUserId = localStorage.getItem(USER_ID_KEY)
    const newUserId = data.user?.id != null ? String(data.user.id) : null
    if (newUserId && prevUserId && prevUserId !== newUserId) {
      USER_SCOPED_KEYS.forEach((key) => localStorage.removeItem(key))
    }
    localStorage.setItem('aichuangzuo_access_token', data.accessToken)
    localStorage.setItem('aichuangzuo_refresh_token', data.refreshToken)
    localStorage.setItem('aichuangzuo_remember_me', data.rememberMe ? 'true' : 'false')
    if (newUserId) {
      localStorage.setItem(USER_ID_KEY, newUserId)
    }
  }

  const handleLogin = () => {
    if (!agreed.value) {
      agreementShakeCount.value++
      message.warning('请先阅读并同意《用户协议》和《隐私政策》')
      return
    }
    openLoginSliderModal()
  }

  const handleRegister = async () => {
    if (!agreed.value) {
      agreementShakeCount.value++
      message.warning('请先阅读并同意《用户协议》和《隐私政策》')
      return
    }
    if (registerMode.value === 'email') {
      if (!registerForm.email) {
        message.warning('请输入邮箱')
        return
      }
      if (!EMAIL_REGEX.test(registerForm.email)) {
        message.warning('邮箱格式不正确')
        return
      }
    } else {
      if (!registerForm.phone) {
        message.warning('请输入手机号')
        return
      }
      if (!PHONE_REGEX.test(registerForm.phone)) {
        message.warning('手机号格式不正确')
        return
      }
    }
    if (!registerForm.code) {
      message.warning(registerMode.value === 'email' ? '请输入邮箱验证码' : '请输入短信验证码')
      return
    }
    if (!/^\d{6}$/.test(registerForm.code)) {
      message.warning('验证码为 6 位数字')
      return
    }
    if (!registerForm.password) {
      message.warning('请输入密码')
      return
    }
    if (registerForm.password.length < 6 || registerForm.password.length > 20) {
      message.warning('密码长度需在 6-20 位之间')
      return
    }
    if (!registerForm.confirmPassword) {
      message.warning('请再次输入确认密码')
      return
    }
    if (registerForm.password !== registerForm.confirmPassword) {
      message.warning('两次输入的密码不一致')
      return
    }

    const payload = {
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      inviteCode: registerForm.inviteCode.trim().toUpperCase() || undefined,
      rememberMe: rememberMe.value
    }
    if (registerMode.value === 'email') {
      payload.email = registerForm.email
      payload.emailCode = registerForm.code
    } else {
      payload.phone = registerForm.phone
      payload.smsCode = registerForm.code
    }

    try {
      const res = await registerApi(payload)
      persistTokens(res.data)
      message.success('注册成功')
      const redirect = router.currentRoute.value.query.redirect
      router.push(typeof redirect === 'string' && redirect ? decodeURIComponent(redirect) : '/console')
    } catch (err) {
      message.error(err?.message || '注册失败')
    }
  }

  onMounted(() => {
    const ref = getRefFromUrl()
    if (ref) {
      registerForm.inviteCode = ref
      showInviteBanner.value = true
      activeTab.value = 'register'
    }
  })

  onBeforeUnmount(() => {
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  })

  return {
    activeTab,
    showInviteBanner,
    agreed,
    agreementShakeCount,
    loginMode,
    registerMode,
    loginForm,
    registerForm,
    loginIdentifier,
    sliderModalVisible,
    sliderModalPassed,
    loginSliderModalVisible,
    loginModalPassed,
    codeCountdown,
    openSliderModal,
    openLoginSliderModal,
    handleLogin,
    handleRegister,
    rememberMe
  }
}
