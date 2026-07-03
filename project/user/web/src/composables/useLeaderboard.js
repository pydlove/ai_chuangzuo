import { ref } from 'vue'

const INCOME_SUBMISSIONS_KEY = 'aichuangzuo_leaderboard_income_submissions'
const REWARD_RECORDS_KEY = 'aichuangzuo_leaderboard_rewards'
const USER_ID_KEY = 'aichuangzuo_user_id'
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const EARNINGS_KEY = 'aichuangzuo_earnings_records'

function load(key, fallback = []) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

function save(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}

export function getUserId() {
  let id = localStorage.getItem(USER_ID_KEY)
  if (!id) {
    id = 'u_' + Math.random().toString(36).slice(2, 10)
    localStorage.setItem(USER_ID_KEY, id)
  }
  return id
}

export const incomeSubmissions = ref(load(INCOME_SUBMISSIONS_KEY))
export const rewardRecords = ref(load(REWARD_RECORDS_KEY))
