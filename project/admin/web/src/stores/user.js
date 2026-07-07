import { defineStore } from 'pinia'
import storage from '@/utils/storage.js'

const TOKEN_KEY = 'admin_access_token'

export const useUserStore = defineStore('adminUser', {
  state: () => ({
    token: storage.get(TOKEN_KEY) || '',
    userInfo: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token
  },

  actions: {
    setToken(token) {
      this.token = token
      storage.set(TOKEN_KEY, token)
    },

    clearToken() {
      this.token = ''
      storage.remove(TOKEN_KEY)
    },

    setUserInfo(info) {
      this.userInfo = info
    }
  }
})
