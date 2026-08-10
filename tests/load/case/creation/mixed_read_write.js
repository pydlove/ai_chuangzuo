// 读写混合压测：多账号 + 读文章 + 用真实标题提交创作任务
// 用法：TARGET_VUS=100 k6 run mixed_read_write.js
// 或：  ./run mixed_read_write.js 100

import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { BASE_URL, getHeaders, ENDPOINTS } from '../../config/config.js';
import { loginWithAccount } from '../../config/auth.js';
import { generateReport } from '../../config/summary.js';
import accountsRaw from '../../config/accounts.json';

// k6 导入 JSON 可能是数组或 { default: 数组 }
const accounts = Array.isArray(accountsRaw) ? accountsRaw : (accountsRaw.default || []);

const targetVus = parseInt(__ENV.TARGET_VUS || '100', 10);
const rampDuration = Math.max(30, Math.floor(targetVus / 10));
const holdDuration = Math.max(60, Math.floor(targetVus / 5));

export function handleSummary(data) {
  return generateReport(data, `mixed-${targetVus}`);
}

export const options = {
  stages: [
    { duration: `${rampDuration}s`, target: targetVus },
    { duration: `${holdDuration}s`, target: targetVus },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.10'],
  },
};

const PLATFORMS = ['wechat', 'xiaohongshu', 'toutiao', 'baijiahao', 'zhihu', 'douyin'];

// 每个 VU 的 token 缓存
const vuTokens = {};

function getTokenForVu(vuId) {
  vuId = parseInt(vuId, 10);

  if (vuTokens[vuId]) {
    return vuTokens[vuId];
  }

  if (!accounts || accounts.length === 0) {
    console.log('[AUTH ERROR] accounts.json 为空，请填写账号');
    return '';
  }

  // 按 VU ID 循环分配账号
  const index = (vuId - 1) % accounts.length;
  const account = accounts[index];

  if (!account) {
    console.log(`[AUTH ERROR] 无法为 VU ${vuId} 分配账号，index=${index}，账号总数=${accounts.length}`);
    return '';
  }

  const token = loginWithAccount(account.email, account.password);
  if (token) {
    console.log(`[AUTH] VU ${vuId} 使用账号: ${account.email}`);
    vuTokens[vuId] = token;
  }
  return token;
}

function fetchTopics(token) {
  const headers = {
    ...getHeaders(),
    Authorization: `Bearer ${token}`,
  };
  const res = http.get(`${BASE_URL}${ENDPOINTS.topicRandom}?count=100`, { headers });

  if (res.status !== 200) {
    console.log(`[TOPIC ERROR] 获取标题失败，Status: ${res.status}`);
    return [];
  }

  try {
    const json = res.json();
    // 用户端接口返回 Result<List<TopicTitleVO>>，data 就是列表
    const topics = json.data || [];
    console.log(`[TOPIC] 成功获取 ${topics.length} 条真实标题`);
    return topics;
  } catch (e) {
    console.log(`[TOPIC ERROR] 解析标题响应失败: ${e.message}`);
    return [];
  }
}

export function setup() {
  console.log(`[CONFIG] 目标并发用户数: ${targetVus}`);
  console.log(`[CONFIG] 账号池数量: ${accounts.length}`);

  let topics = [];
  if (accounts && accounts.length > 0) {
    const token = loginWithAccount(accounts[0].email, accounts[0].password);
    if (token) {
      topics = fetchTopics(token);
    }
  }

  if (topics.length === 0) {
    console.log('[TOPIC WARN] 没有获取到真实标题，将使用占位标题');
  }

  return { topics };
}

function submitTask(headers, topics) {
  let title = `压测文章-${Date.now()}-${randomIntBetween(1000, 9999)}`;
  let description = '压测生成的描述内容';

  // 如果有真实标题，随机选一个
  if (topics && topics.length > 0) {
    const topic = topics[randomIntBetween(0, topics.length - 1)];
    title = topic.title || title;
    description = topic.summary || description;
  }

  const payload = JSON.stringify({
    title,
    platform: PLATFORMS[randomIntBetween(0, PLATFORMS.length - 1)],
    wordCount: randomIntBetween(500, 2000),
    description,
  });

  const res = http.post(`${BASE_URL}${ENDPOINTS.createTask}`, payload, { headers });

  check(res, {
    'create status 200': (r) => r.status === 200,
    'create response < 2s': (r) => r.timings.duration < 2000,
  });
}

function readArticles(headers) {
  const res = http.get(`${BASE_URL}${ENDPOINTS.articleList}`, { headers });

  check(res, {
    'list status 200': (r) => r.status === 200,
    'list response < 500ms': (r) => r.timings.duration < 500,
  });
}

export default function (data) {
  const token = getTokenForVu(__VU);
  const headers = {
    ...getHeaders(),
    Authorization: `Bearer ${token}`,
  };

  // 70% 读，30% 写，模拟真实用户行为
  if (Math.random() < 0.7) {
    readArticles(headers);
  } else {
    submitTask(headers, data.topics);
  }

  sleep(randomIntBetween(1, 3));
}
