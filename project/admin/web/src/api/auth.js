import request from '@/utils/request.js'

const MOCK_ADMIN = {
  account: 'admin',
  password: '123456'
}

function delay(ms = 300 + Math.floor(Math.random() * 300)) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export function adminAuthLogin(data) {
  return delay().then(() => {
    if (data.account === MOCK_ADMIN.account && data.password === MOCK_ADMIN.password) {
      return {
        code: 200,
        message: '登录成功',
        data: {
          token: 'mock-admin-token',
          userInfo: {
            name: '超级管理员',
            account: MOCK_ADMIN.account
          }
        }
      }
    }
    return Promise.reject(new Error('账号或密码错误'))
  })
}
