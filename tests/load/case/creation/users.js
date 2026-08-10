// 指定并发用户数的压测
// 用法：TARGET_VUS=100 k6 run users.js
// 或：  ./run users.js 100

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, getHeaders, ENDPOINTS } from '../../config/config.js';
import { loginAndGetToken } from '../../config/auth.js';
import { generateReport } from '../../config/summary.js';

const targetVus = parseInt(__ENV.TARGET_VUS || '100', 10);
const rampDuration = Math.max(30, Math.floor(targetVus / 10)); // 至少 30 秒 ramp up
const holdDuration = Math.max(60, Math.floor(targetVus / 5));  // 至少 60 秒保持

export function handleSummary(data) {
  return generateReport(data, `users-${targetVus}`);
}

export const options = {
  stages: [
    { duration: `${rampDuration}s`, target: targetVus },  //  ramp up
    { duration: `${holdDuration}s`, target: targetVus },   //  保持目标并发
    { duration: '30s', target: 0 },                        //  收尾
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.10'],
  },
};

export function setup() {
  console.log(`[CONFIG] 目标并发用户数: ${targetVus}`);
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

  sleep(1);
}
