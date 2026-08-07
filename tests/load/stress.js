// 压力测试：持续加压直到系统出现错误/超时，找到崩溃边界
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, getHeaders, ENDPOINTS } from './config.js';
export { handleSummary } from './summary.js';

export const options = {
  stages: [
    { duration: '2m', target: 100 },
    { duration: '2m', target: 200 },
    { duration: '2m', target: 300 },
    { duration: '2m', target: 500 },
    { duration: '2m', target: 800 },
    { duration: '2m', target: 0 },
  ],
  // 压力测试允许一定错误率，阈值放宽
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.10'],
  },
};

export default function () {
  const url = `${BASE_URL}${ENDPOINTS.articleList}`;
  const res = http.get(url, { headers: getHeaders() });

  check(res, {
    'status is 200 or 429/503': (r) =
      r.status === 200 || r.status === 429 || r.status === 503,
  });

  sleep(0.5);
}
