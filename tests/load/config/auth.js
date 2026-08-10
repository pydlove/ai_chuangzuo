// 统一登录逻辑
// 如果 config.js 里配置了 TOKEN，直接返回；否则调登录接口拿 token

import http from 'k6/http';
import {
  AUTH_USERNAME,
  AUTH_PASSWORD,
  AUTH_USERNAME_FIELD,
  AUTH_PASSWORD_FIELD,
  BASE_URL,
  ENDPOINTS,
  TOKEN,
  TOKEN_FIELD_PATH,
} from './config.js';

function getValueByPath(obj, path) {
  if (!path) return undefined;
  return path.split('.').reduce((acc, key) => {
    if (acc && typeof acc === 'object' && key in acc) {
      return acc[key];
    }
    return undefined;
  }, obj);
}

function doLogin(username, password) {
  const payloadObj = {
    [AUTH_USERNAME_FIELD]: username,
    [AUTH_PASSWORD_FIELD]: password,
  };
  const loginPayload = JSON.stringify(payloadObj);
  const loginUrl = `${BASE_URL}${ENDPOINTS.login}`;

  const res = http.post(loginUrl, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  if (res.status !== 200) {
    console.log(`[AUTH ERROR] 登录失败，Status: ${res.status}, Body: ${res.body?.substring(0, 500)}`);
    return '';
  }

  const json = res.json();
  const token = getValueByPath(json, TOKEN_FIELD_PATH) || '';

  if (!token) {
    console.log(`[AUTH ERROR] 未找到 token，请检查 TOKEN_FIELD_PATH: ${TOKEN_FIELD_PATH}`);
    console.log(`[AUTH ERROR] 响应体: ${res.body?.substring(0, 500)}`);
    return '';
  }

  return token;
}

export function loginAndGetToken() {
  // 如果已经配了 token，直接用
  if (TOKEN) {
    console.log('[AUTH] 使用配置的 TOKEN');
    return TOKEN;
  }

  console.log(`[AUTH] 登录 URL: ${BASE_URL}${ENDPOINTS.login}`);
  console.log(`[AUTH] 登录账号: ${AUTH_USERNAME}`);

  const token = doLogin(AUTH_USERNAME, AUTH_PASSWORD);
  if (token) {
    console.log(`[AUTH SUCCESS] token 已获取，长度: ${token.length}`);
  }
  return token;
}

export function loginWithAccount(email, password) {
  return doLogin(email, password);
}
