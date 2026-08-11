// 混合场景压测：模拟真实用户访问多个接口的分布
import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { BASE_URL, getHeaders, ENDPOINTS, DEFAULT_THRESHOLDS } from '../../config/config.js';
import { loginAndGetToken } from '../../config/auth.js';
import { generateReport } from '../../config/summary.js';

export function handleSummary(data) {
  return generateReport(data, 'api_mix');
}

export const options = {
  stages: [
    { duration: '1m', target: 20 },
    { duration: '3m', target: 50 },
    { duration: '5m', target: 50 },
    { duration: '1m', target: 0 },
  ],
  thresholds: DEFAULT_THRESHOLDS,
};

export function setup() {
  return { token: loginAndGetToken() };
}

export default function (data) {
  const headers = {
    ...getHeaders(),
    Authorization: `Bearer ${data.token}`,
  };

  // 用随机数分配请求比例，模拟真实流量分布
  const r = Math.random();

  if (r < 0.6) {
    // 60% 用户刷列表
    const page = randomIntBetween(1, 10);
    const res = http.get(`${BASE_URL}${ENDPOINTS.articleList}?page=${page}&size=20`, {
      headers,
    });
    check(res, {
      'list status 200': (r) => r.status === 200,
      'list p95 < 500ms': (r) => r.timings.duration < 500,
    });
  } else if (r < 0.9) {
    // 30% 用户看详情
    const id = randomIntBetween(1, 1000);
    const res = http.get(`${BASE_URL}${ENDPOINTS.articleDetail.replace('/1', '/' + id)}`, {
      headers,
    });
    check(res, {
      'detail status 200': (r) => r.status === 200,
    });
  } else {
    // 10% 用户提交创作任务
    const payload = JSON.stringify({
      title: `压测文章 ${Date.now()}`,
      platform: 'wechat',
      wordCount: 500,
    });
    const res = http.post(`${BASE_URL}${ENDPOINTS.createTask}`, payload, {
      headers,
    });
    const bodyCode = res.json()?.code;

    check(res, {
      'create accepted or queued': (r) => {
        if (r.status !== 200) return false;
        return bodyCode === 0 || bodyCode === 200 || bodyCode === 212005;
      },
    });

    if (bodyCode === 212005) {
      console.log('[QUEUE FULL] 当前用户队列任务数已达上限，跳过本次写入');
    }
  }

  sleep(randomIntBetween(1, 3));
}
