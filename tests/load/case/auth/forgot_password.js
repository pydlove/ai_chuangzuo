// 忘记密码压测：发送验证码 + 抓取验证码 + 重置密码
// 依赖：accounts.json 里的账号已存在，且服务端启用 test profile
// 用法：./run case/auth/forgot_password.js 50

import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { BASE_URL, getHeaders, ENDPOINTS } from '../../config/config.js';
import { generateReport } from '../../config/summary.js';

const accountsFile = __ENV.ACCOUNTS_FILE || '../../config/accounts.json';
const accountsRaw = JSON.parse(open(accountsFile));
const accounts = Array.isArray(accountsRaw) ? accountsRaw : (accountsRaw.default || []);
console.log(`[ACCOUNTS] 文件: ${accountsFile}, 数量: ${accounts.length}`);

const targetVus = parseInt(__ENV.TARGET_VUS || '50', 10);
const rampDuration = Math.max(30, Math.floor(targetVus / 10));
const holdDuration = Math.max(60, Math.floor(targetVus / 5));

export function handleSummary(data) {
  return generateReport(data, `auth-forgot-password-${targetVus}`);
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
  if (!accounts || accounts.length === 0) {
    console.log('[AUTH ERROR] accounts.json 为空');
    return null;
  }
  const index = (vuId - 1) % accounts.length;
  return accounts[index];
}

function sendEmailCode(email) {
  const res = http.post(
    `${BASE_URL}${ENDPOINTS.sendEmailCode}`,
    JSON.stringify({ email }),
    { headers: getHeaders() }
  );
  const bodyCode = res.json()?.code;
  check(res, {
    'send-code status 200': (r) => r.status === 200,
    'send-code business success': () => bodyCode === 0 || bodyCode === 200,
  });
  return { status: res.status, bodyCode };
}

function fetchTestEmailCode(email) {
  const res = http.get(`${BASE_URL}${ENDPOINTS.testEmailCode}?email=${encodeURIComponent(email)}`, {
    headers: getHeaders(),
  });
  if (res.status !== 200) {
    console.log(`[TEST-CODE ERROR] 无法抓取验证码，Status: ${res.status}`);
    return null;
  }
  try {
    const json = res.json();
    if (!json.found) {
      console.log(`[TEST-CODE ERROR] 未找到验证码: ${email}`);
      return null;
    }
    return json.code;
  } catch (e) {
    console.log(`[TEST-CODE ERROR] 解析失败: ${e.message}`);
    return null;
  }
}

function resetPassword(email, code) {
  const newPassword = 'Aichuangzuo@123';
  const payload = JSON.stringify({
    email,
    emailCode: code,
    password: newPassword,
    confirmPassword: newPassword,
  });
  const res = http.post(`${BASE_URL}${ENDPOINTS.resetPassword}`, payload, { headers: getHeaders() });
  const bodyCode = res.json()?.code;
  check(res, {
    'reset-password status 200': (r) => r.status === 200,
    'reset-password business success': () => bodyCode === 0 || bodyCode === 200,
    'reset-password response < 3s': (r) => r.timings.duration < 3000,
  });
  return { status: res.status, bodyCode };
}

export default function () {
  const account = getAccountForVu(__VU);
  if (!account) {
    sleep(1);
    return;
  }

  const sendResult = sendEmailCode(account.email);
  if (sendResult.status !== 200 || (sendResult.bodyCode !== 0 && sendResult.bodyCode !== 200)) {
    sleep(randomIntBetween(1, 3));
    return;
  }

  sleep(randomIntBetween(1, 2));

  const code = fetchTestEmailCode(account.email);
  if (!code) {
    sleep(randomIntBetween(1, 3));
    return;
  }

  resetPassword(account.email, code);
  sleep(randomIntBetween(1, 3));
}
