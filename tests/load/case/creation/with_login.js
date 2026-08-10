// 需要登录的压测（此脚本已和 smoke.js 等功能合并，保留作为兼容入口）
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, getHeaders, ENDPOINTS } from '../../config/config.js';
import { loginAndGetToken } from '../../config/auth.js';
import { generateReport } from '../../config/summary.js';

export function handleSummary(data) {
  return generateReport(data, 'with_login');
}

export const options = {
  vus: 50,
  duration: '5m',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  return { token: loginAndGetToken() };
}

export default function (data) {
  const headers = {
    ...getHeaders(),
    Authorization: `Bearer ${data.token}`,
  };

  const res = http.get(`${BASE_URL}${ENDPOINTS.articleList}`, { headers });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
