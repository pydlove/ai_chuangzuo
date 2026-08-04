import { ref, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getRefFromUrl } from '@/composables/useInviteCode'
import { sendEmailCode, register as registerApi, login as loginApi } from '@/api/auth'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function useLogin() {
  const router = useRouter()

  const activeTab = ref('login')
  const showInviteBanner = ref(false)
  const agreed = ref(false)
  const agreementShakeCount = ref(0)

  const loginForm = reactive({
    email: 'py_world@163.com',
    password: '123456'
  })

  const registerForm = reactive({
    email: 'py_world@163.com',
    code: '',
    password: '',
    confirmPassword: '',
    inviteCode: ''
  })

  // 注册流程：发送邮箱验证码前的滑块弹框
  const sliderModalVisible = ref(false)
  const sliderModalPassed = ref(false)
  let modalSending = false

  // 登录流程：调用后端登录接口前的滑块弹框
  const loginSliderModalVisible = ref(false)
  const loginModalPassed = ref(false)
  let loginModalSending = false

  // 注册弹框内滑块通过 → 调发送邮箱验证码接口
  watch(sliderModalPassed, async (val) => {
    if (!val || modalSending) return
    modalSending = true
    try {
      await sendEmailCode({ email: registerForm.email })
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
      const res = await loginApi({
        email: loginForm.email,
        password: loginForm.password
      })
      persistTokens(res.data)
      message.success('登录成功')
      loginSliderModalVisible.value = false
      router.push('/console')
    } catch (err) {
      message.error(err?.message || '登录失败')
      loginSliderModalVisible.value = false
    } finally {
      loginModalSending = false
    }
  })

  const openSliderModal = () => {
    if (codeCountdown.value > 0) return
    if (!registerForm.email) {
      message.warning('请先填写邮箱')
      return
    }
    if (!EMAIL_REGEX.test(registerForm.email)) {
      message.warning('邮箱格式不正确')
      return
    }
    sliderModalPassed.value = false
    sliderModalVisible.value = true
  }

  const openLoginSliderModal = () => {
    if (!loginForm.email) {
      message.warning('请填写邮箱')
      return
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
    localStorage.setItem('aichuangzuo_access_token', data.accessToken)
    localStorage.setItem('aichuangzuo_refresh_token', data.refreshToken)
    localStorage.removeItem('aichuangzuo_membership')
    localStorage.removeItem('aichuangzuo_newcomer_modal_dismissed')
    localStorage.removeItem('aichuangzuo_invite_modal_dismissed')
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
    if (!registerForm.email) {
      message.warning('请输入邮箱')
      return
    }
    if (!EMAIL_REGEX.test(registerForm.email)) {
      message.warning('邮箱格式不正确')
      return
    }
    if (!registerForm.code) {
      message.warning('请输入邮箱验证码')
      return
    }
    if (!/^\d{6}$/.test(registerForm.code)) {
      message.warning('邮箱验证码为 6 位数字')
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

    const inviteCode = registerForm.inviteCode.trim().toUpperCase()

    try {
      const res = await registerApi({
        email: registerForm.email,
        emailCode: registerForm.code,
        password: registerForm.password,
        confirmPassword: registerForm.confirmPassword,
        inviteCode: inviteCode || undefined
      })
      persistTokens(res.data)
      message.success('注册成功')
      router.push('/console')
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
    loginForm,
    registerForm,
    sliderModalVisible,
    sliderModalPassed,
    loginSliderModalVisible,
    loginModalPassed,
    codeCountdown,
    openSliderModal,
    openLoginSliderModal,
    handleLogin,
    handleRegister
  }
}
