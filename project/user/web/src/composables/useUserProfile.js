import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  getMyProfile,
  updateNickname,
  updateEmail,
  changePassword,
  bindInviteCode
} from '@/api/user'

// 模块级 ref：单例模式，整个 console 共享一份 profile。
// 不再额外包装 store，因为只有 ConsoleLayout 一处使用。
const profile = ref(null)
const loading = ref(false)

/** 从异常负载里取出可读 message；兼容多种错误结构。 */
function errMsg(e) {
  if (!e) return '请求失败'
  if (typeof e === 'string') return e
  return e.message || e.msg || '请求失败'
}

/**
 * 用户个人资料 composable。
 * 提供全局共享的 profile 引用 + 4 个修改方法（成功本地更新，失败弹 message 并 rethrow）。
 *
 * <p>loadProfile：401 时拦截器跳登录，其他错误静默（不打扰登录态正常的页面）。
 * <p>saveNickname / saveEmail / savePassword：失败时弹 message.error 并 rethrow，
 * 调用方可在 catch 里做"恢复原值"等操作。
 */
export function useUserProfile() {

  async function loadProfile() {
    loading.value = true
    try {
      // auth.js 拦截器返回 {code:0, data:{...}}，再取 data 才是 profile
      const res = await getMyProfile()
      profile.value = res.data || res
    } finally {
      loading.value = false
    }
  }

  async function saveNickname(nickname) {
    const trimmed = nickname.trim()
    try {
      await updateNickname(trimmed)
      if (profile.value) profile.value.nickname = trimmed
      message.success('昵称已更新')
    } catch (e) {
      message.error(errMsg(e))
      throw e
    }
  }

  async function saveEmail(newEmail, emailCode) {
    try {
      await updateEmail(newEmail, emailCode)
      // 服务端把 email_verified 置 1，重新拉一份
      await loadProfile()
      message.success('邮箱已更新')
    } catch (e) {
      message.error(errMsg(e))
      throw e
    }
  }

  async function savePassword(payload) {
    try {
      await changePassword(payload)
      message.success('密码已修改，请重新登录')
      // 密码已改 → 强制下次走 refresh-token 流程；前端只提示，不立即清 token
    } catch (e) {
      message.error(errMsg(e))
      throw e
    }
  }

  async function saveInviteCode(inviteCode) {
    const trimmed = inviteCode.trim().toUpperCase()
    try {
      await bindInviteCode(trimmed)
      await loadProfile()
      message.success('邀请人绑定成功')
    } catch (e) {
      message.error(errMsg(e))
      throw e
    }
  }

  return {
    profile,
    loading,
    loadProfile,
    saveNickname,
    saveEmail,
    savePassword,
    saveInviteCode
  }
}
