// 发送邮箱验证码压测（不需要 test profile）
// 用法：./run case/auth/send_email_code.js 100

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
  return generateReport(data, `auth-send-code-${targetVus}`);
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

export default function () {
  const account = getAccountForVu(__VU);
  if (!account) {
    sleep(1);
    return;
  }

  // 只用真实账号邮箱，避免邮件服务器因假地址投递失败
  const email = account.email;

  const res = http.post(
    `${BASE_URL}${ENDPOINTS.sendEmailCode}`,
    JSON.stringify({ email }),
    { headers: getHeaders() }
  );

  const bodyCode = res.json()?.code;

  check(res, {
    'send-code status 200': (r) => r.status === 200,
    // 111012 = 操作过于频繁（同一邮箱 24h 限 10 次），属于正常保护，不算失败
    'send-code accepted or rate-limited': () =>
      bodyCode === 0 || bodyCode === 200 || bodyCode === 111012,
    'send-code response < 3s': (r) => r.timings.duration < 3000,
  });

  if (bodyCode === 111012) {
    console.log(`[RATE LIMIT] 邮箱 ${email} 发送过于频繁`);
  }

  sleep(randomIntBetween(1, 3));
}
