
import { ref, reactive, watch, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { STORAGE_KEYS, USER_SCOPED_STORAGE_KEYS } from '@/constants/storage.js'
import { getRefFromUrl, getExperienceTokenFromUrl } from '@/composables/useInviteCode'
import { sendEmailCode, sendSmsCode, register as registerApi, login as loginApi } from '@/api/auth'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE_REGEX = /^1[3-9]\d{9}$/

// 用户作用域的本地缓存：换账号时一并清空
const USER_ID_KEY = STORAGE_KEYS.USER_ID

export function persistTokens(data) {
  const prevUserId = localStorage.getItem(USER_ID_KEY)
  const newUserId = data.user?.id != null ? String(data.user.id) : null
  if (newUserId && prevUserId && prevUserId !== newUserId) {
    USER_SCOPED_STORAGE_KEYS.forEach((key) => localStorage.removeItem(key))
  }
  localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, data.accessToken)
  localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, data.refreshToken)
  localStorage.setItem(STORAGE_KEYS.REMEMBER_ME, data.rememberMe ? 'true' : 'false')
  if (newUserId) {
    localStorage.setItem(USER_ID_KEY, newUserId)
  }
}

export function useLogin() {
  const router = useRouter()

  const activeTab = ref('login')
  const showInviteBanner = ref(false)
  const showExperienceBanner = ref(false)
  const agreed = ref(false)
  const agreementShakeCount = ref(0)

  const loginForm = reactive({
    identifier: '',
    password: ''
  })

  const registerForm = reactive({
    identifier: '',
    code: '',
    password: '',
    confirmPassword: '',
    inviteCode: '',
    experienceToken: ''
  })

  const detectMode = (value) => {
    if (EMAIL_REGEX.test(value)) return 'email'
    if (PHONE_REGEX.test(value)) return 'phone'
    return 'unknown'
  }

  const loginMode = computed(() => detectMode(loginForm.identifier))
  const registerMode = computed(() => detectMode(registerForm.identifier))
  const loginIdentifier = computed(() => loginForm.identifier)

  // 注册流程：发送验证码前的滑块弹框
  const sliderModalVisible = ref(false)
  const sliderModalPassed = ref(false)
  let modalSending = false

  // 登录流程：调用后端登录接口前的滑块弹框
  const loginSliderModalVisible = ref(false)
  const loginModalPassed = ref(false)
  let loginModalSending = false

  let isMounted = true

  const rememberMe = ref(localStorage.getItem(STORAGE_KEYS.REMEMBER_ME) === 'true')

  watch(rememberMe, (val) => {
    localStorage.setItem(STORAGE_KEYS.REMEMBER_ME, val ? 'true' : 'false')
  })

  const normalizeLoginForm = () => {
    loginForm.identifier = loginForm.identifier.trim()
    loginForm.password = loginForm.password.trim()
  }

  const normalizeRegisterForm = () => {
    registerForm.identifier = registerForm.identifier.trim()
    registerForm.code = registerForm.code.trim()
    registerForm.password = registerForm.password.trim()
    registerForm.confirmPassword = registerForm.confirmPassword.trim()
    registerForm.inviteCode = registerForm.inviteCode.trim()
    registerForm.experienceToken = registerForm.experienceToken.trim()
  }

  // 注册弹框内滑块通过 → 发送验证码
  watch(sliderModalPassed, async (val) => {
    if (!val || modalSending) return
    const mode = registerMode.value
    if (mode === 'unknown') return
    modalSending = true
    try {
      if (mode === 'email') {
        await sendEmailCode({ email: registerForm.identifier })
      } else {
        await sendSmsCode({ phone: registerForm.identifier })
      }
      if (!isMounted) return
      startCodeCountdown()
      message.success('验证码已发送')
    } catch (err) {
      if (!isMounted) return
      message.error(err?.message || '发送失败')
    } finally {
      if (!isMounted) return
      sliderModalVisible.value = false
      modalSending = false
    }
  })

  // 登录弹框内滑块通过 → 调后端登录接口
  watch(loginModalPassed, async (val) => {
    if (!val || loginModalSending) return
    const mode = loginMode.value
    if (mode === 'unknown') return
    loginModalSending = true
    try {
      const payload = { password: loginForm.password, rememberMe: rememberMe.value }
      if (mode === 'email') {
        payload.email = loginForm.identifier
      } else {
        payload.phone = loginForm.identifier
      }
      const res = await loginApi(payload)
      if (!isMounted) return
      persistTokens(res.data)
      message.success('登录成功')
      loginSliderModalVisible.value = false
      const redirect = router.currentRoute.value.query.redirect
      router.push(typeof redirect === 'string' && redirect ? decodeURIComponent(redirect) : '/console/workbench')
    } catch (err) {
      if (!isMounted) return
      message.error(err?.message || '登录失败')
      loginSliderModalVisible.value = false
    } finally {
      if (!isMounted) return
      loginModalSending = false
    }
  })

  const openSliderModal = () => {
    if (codeCountdown.value > 0) return
    normalizeRegisterForm()
    const mode = registerMode.value
    if (mode === 'unknown') {
      message.warning('请输入有效的手机号或邮箱')
      return
    }
    if (mode === 'email') {
      if (!EMAIL_REGEX.test(registerForm.identifier)) {
        message.warning('邮箱格式不正确')
        return
      }
    } else {
      if (!PHONE_REGEX.test(registerForm.identifier)) {
        message.warning('手机号格式不正确')
        return
      }
    }
    sliderModalPassed.value = false
    sliderModalVisible.value = true
  }

  const openLoginSliderModal = () => {
    normalizeLoginForm()
    const mode = loginMode.value
    if (mode === 'unknown') {
      message.warning('请输入有效的手机号或邮箱')
      return
    }
    if (mode === 'email') {
      if (!EMAIL_REGEX.test(loginForm.identifier)) {
        message.warning('邮箱格式不正确')
        return
      }
    } else {
      if (!PHONE_REGEX.test(loginForm.identifier)) {
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

  const handleLogin = () => {
    normalizeLoginForm()
    if (!agreed.value) {
      agreementShakeCount.value++
      message.warning('请先阅读并同意《用户协议》和《隐私政策》')
      return
    }
    openLoginSliderModal()
  }

  const handleRegister = async () => {
    normalizeRegisterForm()
    if (!agreed.value) {
      agreementShakeCount.value++
      message.warning('请先阅读并同意《用户协议》和《隐私政策》')
      return
    }
    const mode = registerMode.value
    if (mode === 'unknown') {
      message.warning('请输入有效的手机号或邮箱')
      return
    }
    if (mode === 'email') {
      if (!EMAIL_REGEX.test(registerForm.identifier)) {
        message.warning('邮箱格式不正确')
        return
      }
    } else {
      if (!PHONE_REGEX.test(registerForm.identifier)) {
        message.warning('手机号格式不正确')
        return
      }
    }
    if (!registerForm.code) {
      message.warning('请输入验证码')
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
      inviteCode: registerForm.inviteCode.toUpperCase() || undefined,
      experienceToken: registerForm.experienceToken || undefined,
      rememberMe: rememberMe.value
    }
    if (mode === 'email') {
      payload.email = registerForm.identifier
      payload.emailCode = registerForm.code
    } else {
      payload.phone = registerForm.identifier
      payload.smsCode = registerForm.code
    }

    try {
      const res = await registerApi(payload)
      persistTokens(res.data)
      message.success('注册成功')
      const redirect = router.currentRoute.value.query.redirect
      router.push(typeof redirect === 'string' && redirect ? decodeURIComponent(redirect) : '/console/workbench')
    } catch (err) {
      message.error(err?.message || '注册失败')
    }
  }

  onMounted(() => {
    isMounted = true
    const experienceToken = getExperienceTokenFromUrl()
    if (experienceToken) {
      registerForm.experienceToken = experienceToken
      showExperienceBanner.value = true
      activeTab.value = 'register'
      return
    }
    const ref = getRefFromUrl()
    if (ref) {
      registerForm.inviteCode = ref
      showInviteBanner.value = true
      activeTab.value = 'register'
    }
  })

  onBeforeUnmount(() => {
    isMounted = false
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  })

  return {
    activeTab,
    showInviteBanner,
    showExperienceBanner,
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
