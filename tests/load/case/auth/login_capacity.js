// 登录容量测试：阶梯加压，观察从多少并发开始响应时间急剧上升
// 用法：k6 run case/auth/login_capacity.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { BASE_URL, getHeaders, ENDPOINTS } from '../../config/config.js';
import { generateReport } from '../../config/summary.js';

const accountsFile = __ENV.ACCOUNTS_FILE || '../../config/accounts.json';
const accountsRaw = JSON.parse(open(accountsFile));
const accounts = Array.isArray(accountsRaw) ? accountsRaw : (accountsRaw.default || []);
console.log(`[ACCOUNTS] 文件: ${accountsFile}, 数量: ${accounts.length}`);

export function handleSummary(data) {
  return generateReport(data, 'auth-login-capacity');
}

export const options = {
  stages: [
    { duration: '1m', target: 5 },
    { duration: '1m', target: 10 },
    { duration: '1m', target: 20 },
    { duration: '1m', target: 30 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.10'],
  },
};

function getAccountForVu(vuId) {
  if (!accounts || accounts.length === 0) return null;
  const index = (vuId - 1) % accounts.length;
  return accounts[index];
}

export default function () {
  const account = getAccountForVu(__VU);
  if (!account) {
    sleep(1);
    return;
  }

  const payload = JSON.stringify({
    email: account.email,
    password: account.password,
  });

  const res = http.post(`${BASE_URL}${ENDPOINTS.login}`, payload, {
    headers: getHeaders(),
  });

  const bodyCode = res.json()?.code;
  check(res, {
    'login status 200': (r) => r.status === 200,
    'login business success': () => bodyCode === 0 || bodyCode === 200,
    'login response < 2s': (r) => r.timings.duration < 2000,
  });

  sleep(randomIntBetween(1, 3));
}
