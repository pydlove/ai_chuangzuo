#!/usr/bin/env node
/**
 * 从管理端拉取用户列表，写入 accounts.json 供压测使用
 *
 * 用法：
 *   node fetch_users.js
 *   ADMIN_PASSWORD=xxx USER_COUNT=100 node fetch_users.js
 */

const fs = require('fs');
const http = require('http');
const path = require('path');

// 默认走管理端 Nginx 入口（22347），不直连后端 26060
const ADMIN_BASE_URL = process.env.ADMIN_BASE_URL || 'http://101.126.15.58:22347';
const ADMIN_USERNAME = process.env.ADMIN_USERNAME || 'admin';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'Root1qaz!QAZ';
const USER_COUNT = parseInt(process.env.USER_COUNT || '50', 10);

// 是否给拉取的用户设置套餐和过期时间
const SET_MEMBERSHIP = process.env.SET_MEMBERSHIP !== 'false';
const MEMBERSHIP_PLAN = process.env.MEMBERSHIP_PLAN || 'pro';
const EXPIRE_DATE = process.env.EXPIRE_DATE || '2026-12-31';

function request(method, path, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, ADMIN_BASE_URL);
    const options = {
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      method,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        ...headers,
      },
    };

    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(data) });
        } catch (e) {
          resolve({ status: res.statusCode, body: data });
        }
      });
    });

    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

async function login() {
  console.log(`[ADMIN LOGIN] ${ADMIN_BASE_URL}/api/v1/admin/auth/login`);
  const res = await request('POST', '/api/v1/admin/auth/login', {
    username: ADMIN_USERNAME,
    password: ADMIN_PASSWORD,
  });

  if (res.status !== 200) {
    console.error('[ADMIN LOGIN ERROR]', res.status, res.body);
    throw new Error('管理员登录失败');
  }

  const token = res.body?.data?.accessToken || res.body?.token || res.body?.data?.token;
  if (!token) {
    console.error('[ADMIN LOGIN ERROR] 响应中没有 token:', res.body);
    throw new Error('无法获取管理员 token');
  }

  console.log('[ADMIN LOGIN] 成功');
  return token;
}

async function listUsers(token) {
  const path = `/api/v1/admin/users?page=1&pageSize=${USER_COUNT}`;
  console.log(`[LIST USERS] ${path}`);
  const res = await request('GET', path, null, { Authorization: `Bearer ${token}` });

  if (res.status !== 200) {
    console.error('[LIST USERS ERROR]', res.status, res.body);
    throw new Error('获取用户列表失败');
  }

  const total = res.body?.data?.total || 0;
  const list = res.body?.data?.list || [];
  console.log(`[LIST USERS] 总数: ${total}, 本页返回: ${list.length}`);
  if (list.length > 0) {
    console.log(`[LIST USERS] 前几条: ${list.slice(0, 3).map(u => u.email).join(', ')}`);
  }
  return list;
}

async function updateMembership(token, user) {
  const path = `/api/v1/admin/users/${user.id}`;
  const body = {
    email: user.email,
    nickname: user.nickname || user.email.split('@')[0],
    status: user.status || 'enabled',
    userType: 1, // 1 = 真实用户
    expireDate: EXPIRE_DATE,
    membershipPlan: MEMBERSHIP_PLAN,
  };

  const res = await request('PUT', path, body, { Authorization: `Bearer ${token}` });

  if (res.status !== 200) {
    console.error(`[UPDATE MEMBERSHIP ERROR] 用户 ${user.email}`, res.status, res.body);
    return false;
  }

  console.log(`[UPDATE MEMBERSHIP] ${user.email} -> ${MEMBERSHIP_PLAN}, 到期: ${EXPIRE_DATE}`);
  return true;
}

async function ensurePlanBenefit(token) {
  const body = {
    planKey: MEMBERSHIP_PLAN,
    benefitCode: 'ai_article_quota',
    benefitValue: '10000',
  };
  console.log(`[ENSURE PLAN BENEFIT] ${MEMBERSHIP_PLAN} -> ai_article_quota = 10000`);
  const res = await request('POST', '/api/v1/admin/plan-benefits', body, {
    Authorization: `Bearer ${token}`,
  });

  if (res.status !== 200) {
    console.error('[ENSURE PLAN BENEFIT ERROR]', res.status, res.body);
    throw new Error('确保套餐权益失败');
  }

  console.log('[ENSURE PLAN BENEFIT] 成功');
}

async function main() {
  const token = await login();

  // 确保当前套餐有生成文章的权益，否则创作任务会报 118002
  await ensurePlanBenefit(token);

  const users = await listUsers(token);

  if (users.length === 0) {
    console.error('[ERROR] 没有获取到用户');
    process.exit(1);
  }

  const accounts = [];
  let skipped = 0;
  for (const user of users) {
    if (!user.email) {
      skipped++;
      continue;
    }

    if (SET_MEMBERSHIP) {
      await updateMembership(token, user);
    }

    accounts.push({
      email: user.email,
      password: '123456',
    });
  }

  const outputPath = path.join(__dirname, 'accounts.json');
  fs.writeFileSync(
    outputPath,
    JSON.stringify(accounts, null, 2)
  );

  console.log(`[DONE] 已写入 ${accounts.length} 个用户到 ${outputPath}`);
  if (skipped > 0) {
    console.log(`[WARN] 跳过了 ${skipped} 个没有 email 的用户`);
  }
}

main().catch((err) => {
  console.error(err.message);
  process.exit(1);
});
