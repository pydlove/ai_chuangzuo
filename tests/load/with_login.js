// 需要登录的压测：setup 阶段登录拿 token，VU 阶段带 token 请求
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, getHeaders, ENDPOINTS } from './config.js';
export { handleSummary } from './summary.js';

export const options = {
  vus: 50,
  duration: '5m',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

// 所有 VU 共享：先登录一次拿到 token
export function setup() {
  const loginPayload = JSON.stringify({
    username: __ENV.USERNAME || 'test',
    password: __ENV.PASSWORD || '123456',
  });

  const res = http.post(`${BASE_URL}${ENDPOINTS.login}`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'login success': (r) => r.status === 200,
  });

  // 根据你后端实际响应结构取 token
  const json = res.json();
  const token = json.data?.token || json.token || json.accessToken || '';
  return { token };
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
