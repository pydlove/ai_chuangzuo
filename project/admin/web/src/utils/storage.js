const storage = {
  get(key) {
    try {
      const value = localStorage.getItem(key)
      return value ? JSON.parse(value) : null
    } catch {
      return null
    }
  },

  set(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value))
    } catch {
      // ignore
    }
  },

  remove(key) {
    try {
      localStorage.removeItem(key)
    } catch {
      // ignore
    }
  }
}

export default storage
