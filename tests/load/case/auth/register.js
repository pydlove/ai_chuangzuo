// 注册压测：发送验证码 + 抓取验证码 + 注册
// 依赖：服务端启用 test profile，/api/v1/user/auth 可访问
// 用法：./run case/auth/register.js 50

import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { BASE_URL, getHeaders, ENDPOINTS } from '../../config/config.js';
import { generateReport } from '../../config/summary.js';

const targetVus = parseInt(__ENV.TARGET_VUS || '50', 10);
const rampDuration = Math.max(30, Math.floor(targetVus / 10));
const holdDuration = Math.max(60, Math.floor(targetVus / 5));

export function handleSummary(data) {
  return generateReport(data, `auth-register-${targetVus}`);
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

function generateEmail(vuId, iter) {
  const ts = Date.now();
  return `loadtest_${vuId}_${iter}_${ts}@example.com`;
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

function register(email, code) {
  const password = 'Aichuangzuo@123';
  const payload = JSON.stringify({
    email,
    emailCode: code,
    password,
    confirmPassword: password,
  });
  const res = http.post(`${BASE_URL}${ENDPOINTS.register}`, payload, { headers: getHeaders() });
  const bodyCode = res.json()?.code;
  check(res, {
    'register status 200': (r) => r.status === 200,
    'register business success': () => bodyCode === 0 || bodyCode === 200,
    'register response < 3s': (r) => r.timings.duration < 3000,
  });
  return { status: res.status, bodyCode };
}

export default function () {
  const email = generateEmail(__VU, __ITER);

  const sendResult = sendEmailCode(email);
  if (sendResult.status !== 200 || (sendResult.bodyCode !== 0 && sendResult.bodyCode !== 200)) {
    sleep(randomIntBetween(1, 3));
    return;
  }

  // 稍微等待邮件投递（GreenMail 是内存的，通常不需要）
  sleep(randomIntBetween(1, 2));

  const code = fetchTestEmailCode(email);
  if (!code) {
    sleep(randomIntBetween(1, 3));
    return;
  }

  register(email, code);
  sleep(randomIntBetween(1, 3));
}
