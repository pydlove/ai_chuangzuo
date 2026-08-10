// 负载测试：阶梯式加压，找到系统稳定运行的最大并发
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, getHeaders, ENDPOINTS, DEFAULT_THRESHOLDS } from '../../config/config.js';
import { loginAndGetToken } from '../../config/auth.js';
import { generateReport } from '../../config/summary.js';

export function handleSummary(data) {
  return generateReport(data, 'loadtest');
}

export const options = {
  stages: [
    { duration: '1m', target: 10 },   // 1分钟 ramp up 到 10 并发
    { duration: '3m', target: 10 },   // 保持 3 分钟，观察稳定状态
    { duration: '1m', target: 50 },   // 加压到 50 并发
    { duration: '3m', target: 50 },
    { duration: '1m', target: 100 },  // 加压到 100 并发
    { duration: '3m', target: 100 },
    { duration: '1m', target: 0 },    // 逐步降载
  ],
  thresholds: DEFAULT_THRESHOLDS,
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
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1); // 每个 VU 每秒发一次请求，模拟真实用户间隔
}
