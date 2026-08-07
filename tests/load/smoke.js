// 冒烟测试：1-5 个并发跑 30 秒，验证接口通不通、基本延迟是否正常
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, getHeaders, ENDPOINTS, DEFAULT_THRESHOLDS } from './config.js';
export { handleSummary } from './summary.js';

export const options = {
  vus: 5,
  duration: '30s',
  thresholds: DEFAULT_THRESHOLDS,
};

export default function () {
  // 你可以把这里改成你要压的真实接口
  const url = `${BASE_URL}${ENDPOINTS.articleList}`;
  const res = http.get(url, { headers: getHeaders() });

  // 调试：如果状态码不是 200，打印出来方便定位
  if (res.status !== 200) {
    console.log(`[DEBUG] URL: ${url}, Status: ${res.status}, Body: ${res.body?.substring(0, 200)}`);
  }

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
