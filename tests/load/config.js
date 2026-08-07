/**
 * 压测配置文件
 *
 * 用法一：直接修改下面的 SERVER_IP / SERVER_PORT / PROTOCOL
 * 用法二：通过环境变量覆盖，例如：
 *   BASE_URL=http://192.168.1.100:8080 TOKEN=xxx k6 run smoke.js
 */

// ==========================================
// 在这里修改你的服务器配置
// ==========================================
const SERVER_IP = '101.126.15.58';   // 服务器 IP，例如 '192.168.1.100'
const SERVER_PORT = '22345';      // 服务端口号
const PROTOCOL = 'http';         // http 或 https
// ==========================================

// 最终请求地址，优先读取环境变量 BASE_URL
export const BASE_URL = __ENV.BASE_URL || `${PROTOCOL}://${SERVER_IP}:${SERVER_PORT}`;

// 登录 token，需要鉴权时填写；也可通过环境变量 TOKEN 传入
export const TOKEN = __ENV.TOKEN || '';

// 默认公共请求头
export function getHeaders() {
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };
  if (TOKEN) {
    headers['Authorization'] = `Bearer ${TOKEN}`;
  }
  return headers;
}

// 常见接口路径示例，按需修改成你真实的接口
export const ENDPOINTS = {
  // 健康检查
  health: '/actuator/health',
  // 文章列表（改成你真实的接口）
  articleList: '/api/v1/learn/articles',
  // 文章详情
  articleDetail: '/api/v1/learn/articles/1',
  // 用户登录
  login: '/api/v1/auth/login',
  // 创作任务提交
  createTask: '/api/v1/create/task',
};

// 通用阈值：P95 < 500ms，错误率 < 1%
export const DEFAULT_THRESHOLDS = {
  http_req_duration: ['p(95)<500'],
  http_req_failed: ['rate<0.01'],
};
