// 混合场景压测：模拟真实用户访问多个接口的分布
import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { BASE_URL, getHeaders, ENDPOINTS, DEFAULT_THRESHOLDS } from './config.js';
export { handleSummary } from './summary.js';

export const options = {
  stages: [
    { duration: '1m', target: 20 },
    { duration: '3m', target: 50 },
    { duration: '5m', target: 50 },
    { duration: '1m', target: 0 },
  ],
  thresholds: DEFAULT_THRESHOLDS,
};

export default function () {
  // 用随机数分配请求比例，模拟真实流量分布
  const r = Math.random();

  if (r < 0.6) {
    // 60% 用户刷列表
    const page = randomIntBetween(1, 10);
    const res = http.get(`${BASE_URL}${ENDPOINTS.articleList}?page=${page}&size=20`, {
      headers: getHeaders(),
    });
    check(res, {
      'list status 200': (r) => r.status === 200,
      'list p95 < 500ms': (r) => r.timings.duration < 500,
    });
  } else if (r < 0.9) {
    // 30% 用户看详情
    const id = randomIntBetween(1, 1000);
    const res = http.get(`${BASE_URL}/api/v1/learn/articles/${id}`, {
      headers: getHeaders(),
    });
    check(res, {
      'detail status 200': (r) => r.status === 200,
    });
  } else {
    // 10% 用户提交创作任务
    const payload = JSON.stringify({
      title: `压测文章 ${Date.now()}`,
      platform: 'wechat',
      wordCount: 1000,
    });
    const res = http.post(`${BASE_URL}${ENDPOINTS.createTask}`, payload, {
      headers: getHeaders(),
    });
    check(res, {
      'create status 200': (r) => r.status === 200,
    });
  }

  sleep(randomIntBetween(1, 3));
}
