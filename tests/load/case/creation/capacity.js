// 容量测试：从 50 并发逐步加到 1000 并发，找到系统能扛住的最大并发数
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, getHeaders, ENDPOINTS } from '../../config/config.js';
import { loginAndGetToken } from '../../config/auth.js';
import { generateReport } from '../../config/summary.js';

export function handleSummary(data) {
  return generateReport(data, 'capacity');
}

export const options = {
  stages: [
    { duration: '1m', target: 50 },    // 热身：50 并发
    { duration: '2m', target: 100 },   // 100 并发
    { duration: '2m', target: 200 },   // 200 并发
    { duration: '2m', target: 500 },   // 500 并发
    { duration: '3m', target: 1000 },  // 1000 并发
    { duration: '2m', target: 0 },     // 收尾降载
  ],
  // 容量测试阈值放宽，重点关注拐点而不是绝对通过
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.10'],
  },
};

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
  });

  sleep(1); // 模拟真实用户每隔 1 秒操作一次
}
