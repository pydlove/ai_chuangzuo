// 认证混合压测：登录 + 发验证码 + 注册 + 忘记密码
// 用法：./run case/auth/auth_mix.js 100

import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { BASE_URL, getHeaders, ENDPOINTS } from '../../config/config.js';
import { generateReport } from '../../config/summary.js';

const accountsFile = __ENV.ACCOUNTS_FILE || '../../config/accounts.json';
const accountsRaw = JSON.parse(open(accountsFile));
const accounts = Array.isArray(accountsRaw) ? accountsRaw : (accountsRaw.default || []);
console.log(`[ACCOUNTS] 文件: ${accountsFile}, 数量: ${accounts.length}`);

const targetVus = parseInt(__ENV.TARGET_VUS || '100', 10);
const rampDuration = Math.max(30, Math.floor(targetVus / 10));
const holdDuration = Math.max(60, Math.floor(targetVus / 5));

export function handleSummary(data) {
  return generateReport(data, `auth-mix-${targetVus}`);
}

export const options = {
  stages: [
    { duration: `${rampDuration}s`, target: targetVus },
    { duration: `${holdDuration}s`, target: targetVus },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.10'],
  },
};

function getAccountForVu(vuId) {
  if (!accounts || accounts.length === 0) return null;
  const index = (vuId - 1) % accounts.length;
  return accounts[index];
}

function generateRegisterEmail(vuId, iter) {
  return `loadtest_${vuId}_${iter}_${Date.now()}@example.com`;
}

function checkAuthSuccess(res, name) {
  const bodyCode = res.json()?.code;
  check(res, {
    [`${name} status 200`]: (r) => r.status === 200,
    [`${name} business success`]: () => bodyCode === 0 || bodyCode === 200,
    [`${name} response < 3s`]: (r) => r.timings.duration < 3000,
  });
  return { status: res.status, bodyCode };
}

function login(email, password) {
  const res = http.post(
    `${BASE_URL}${ENDPOINTS.login}`,
    JSON.stringify({ email, password }),
    { headers: getHeaders() }
  );
  return checkAuthSuccess(res, 'login');
}

function sendEmailCode(email) {
  const res = http.post(
    `${BASE_URL}${ENDPOINTS.sendEmailCode}`,
    JSON.stringify({ email }),
    { headers: getHeaders() }
  );
  const bodyCode = res.json()?.code;
  // 111012 = 操作过于频繁，按真实限流策略属于正常拒绝，不算失败
  check(res, {
    'send-code status 200': (r) => r.status === 200,
    'send-code accepted or rate-limited': () =>
      bodyCode === 0 || bodyCode === 200 || bodyCode === 111012,
    'send-code response < 3s': (r) => r.timings.duration < 3000,
  });
  return { status: res.status, bodyCode };
}

function fetchTestEmailCode(email) {
  const res = http.get(`${BASE_URL}${ENDPOINTS.testEmailCode}?email=${encodeURIComponent(email)}`, {
    headers: getHeaders(),
  });
  if (res.status !== 200) return null;
  try {
    const json = res.json();
    return json.found ? json.code : null;
  } catch (e) {
    return null;
  }
}

function register(email, code) {
  const password = 'Aichuangzuo@123';
  const res = http.post(
    `${BASE_URL}${ENDPOINTS.register}`,
    JSON.stringify({ email, emailCode: code, password, confirmPassword: password }),
    { headers: getHeaders() }
  );
  return checkAuthSuccess(res, 'register');
}

function resetPassword(email, code) {
  const password = 'Aichuangzuo@123';
  const res = http.post(
    `${BASE_URL}${ENDPOINTS.resetPassword}`,
    JSON.stringify({ email, emailCode: code, password, confirmPassword: password }),
    { headers: getHeaders() }
  );
  return checkAuthSuccess(res, 'reset-password');
}

export default function () {
  const account = getAccountForVu(__VU);
  const r = Math.random();

  if (r < 0.6) {
    // 60% 登录
    if (account) {
      login(account.email, account.password);
    }
  } else if (r < 0.75) {
    // 15% 注册完整流程
    const email = generateRegisterEmail(__VU, __ITER);
    const sendResult = sendEmailCode(email);
    if (sendResult.status === 200 && (sendResult.bodyCode === 0 || sendResult.bodyCode === 200)) {
      sleep(randomIntBetween(1, 2));
      const code = fetchTestEmailCode(email);
      if (code) {
        register(email, code);
      }
    }
  } else if (r < 0.9) {
    // 15% 忘记密码完整流程
    if (account) {
      const sendResult = sendEmailCode(account.email);
      if (sendResult.status === 200 && (sendResult.bodyCode === 0 || sendResult.bodyCode === 200)) {
        sleep(randomIntBetween(1, 2));
        const code = fetchTestEmailCode(account.email);
        if (code) {
          resetPassword(account.email, code);
        }
      }
    }
  } else {
    // 10% 只发验证码（不校验注册/重置）
    if (account) {
      sendEmailCode(account.email);
    }
  }

  sleep(randomIntBetween(1, 3));
}
