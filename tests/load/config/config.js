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
const SERVER_IP = '101.126.15.58';   // 服务器 IP
// 用户端端口说明：
//   22345 = 用户端前端 Nginx（推荐，走和真实用户一样的入口）
//   25050 = 用户端后端 Spring Boot（直连后端，绕过 Nginx）
const SERVER_PORT = '22345';         // 服务端口号
const PROTOCOL = 'http';             // http 或 https
// ==========================================

// 最终请求地址，优先读取环境变量 BASE_URL
export const BASE_URL = __ENV.BASE_URL || `${PROTOCOL}://${SERVER_IP}:${SERVER_PORT}`;

// ==========================================
// 在这里修改你的登录配置（需要鉴权时填写）
// ==========================================
// 测试账号密码
export const AUTH_USERNAME = __ENV.USERNAME || 'py_world@163.com';
export const AUTH_PASSWORD = __ENV.PASSWORD || '123456';

// 登录请求参数字段名
// 用户端登录用 email + password
export const AUTH_USERNAME_FIELD = 'email';
export const AUTH_PASSWORD_FIELD = 'password';

// 登录成功后，token 在响应 JSON 里的字段路径
// 用户端：Result<AuthTokenVO>，token 字段是 data.accessToken
export const TOKEN_FIELD_PATH = 'data.accessToken';

// 如果你已经有现成的 token，也可以直接填这里，会优先使用
export const TOKEN = __ENV.TOKEN || '';
// ==========================================

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
  // 用户端推荐文章列表（公开接口，无需登录）
  articleList: '/api/v1/user/learn/article/recommended',
  // 用户端文章详情（公开接口）
  articleDetail: '/api/v1/user/learn/article/1',
  // 用户端登录
  login: '/api/v1/user/auth/login',
  // 用户端随机标题（需要登录）
  topicRandom: '/api/v1/user/topics/random',
  // 用户端提交创作任务（需要登录）
  createTask: '/api/v1/user/generation-tasks',
  // 认证相关
  sendEmailCode: '/api/v1/user/auth/email-codes',
  register: '/api/v1/user/auth/register',
  resetPassword: '/api/v1/user/auth/reset-password',
  refreshToken: '/api/v1/user/auth/refresh-token',
  // 仅 test profile 可用，用于抓取 GreenMail 里的 6 位验证码
  testEmailCode: '/__test/email-code',
};

// 通用阈值：P95 < 500ms，错误率 < 1%
export const DEFAULT_THRESHOLDS = {
  http_req_duration: ['p(95)<500'],
  http_req_failed: ['rate<0.01'],
};
