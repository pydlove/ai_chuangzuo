// 冒烟测试：自动登录 + 压测接口
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, getHeaders, ENDPOINTS, DEFAULT_THRESHOLDS } from '../../config/config.js';
import { loginAndGetToken } from '../../config/auth.js';
import { generateReport } from '../../config/summary.js';

export function handleSummary(data) {
  return generateReport(data, 'smoke');
}

export const options = {
  vus: 5,
  duration: '30s',
  thresholds: DEFAULT_THRESHOLDS,
};

// 自动登录，所有 VU 共用同一个 token
export function setup() {
  return { token: loginAndGetToken() };
}

export default function (data) {
  const url = `${BASE_URL}${ENDPOINTS.articleList}`;
  const headers = {
    ...getHeaders(),
    Authorization: `Bearer ${data.token}`,
  };
  const res = http.get(url, { headers });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
