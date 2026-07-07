# 提现页面反洗钱协议勾选功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在「创作币 & 提现」页的申请提现弹框中增加《提现服务协议》勾选，未勾选时禁止提交，并记住用户的同意状态。

**Architecture:** 在 `WithdrawIndex.vue` 组件内增加协议勾选行、协议全文弹框，以及 `localStorage` 持久化。勾选状态参与提交按钮的可用性计算。

**Tech Stack:** Vue 3 Composition API、Ant Design Vue、localStorage

## Global Constraints

- 协议条款使用通用模板，需法务审核后替换为正式文本。
- 用户同意状态保存在 `localStorage` 中，键名为 `aichuangzuo_withdraw_agreement_accepted`。
- 未勾选协议时，「提交申请」按钮必须禁用，且点击提交时给出明确提示。

---

## File Structure

- `project/user/web/src/views/console/WithdrawIndex.vue`
  - 模板：在申请提现弹框内增加协议勾选行与协议全文弹框
  - 脚本：增加 `agreementAccepted` ref、localStorage 读写、提交校验
  - 样式：增加勾选行与协议弹框样式

---

### Task 1: 增加协议状态存储与读取

**Files:**
- Modify: `project/user/web/src/views/console/WithdrawIndex.vue`

**Interfaces:**
- Consumes: 无
- Produces: `agreementAccepted` ref、`loadAgreement()`、`saveAgreement()`

- [ ] **Step 1: 在 script setup 顶部增加 localStorage key 常量**

```javascript
const WITHDRAW_AGREEMENT_KEY = 'aichuangzuo_withdraw_agreement_accepted'
```

- [ ] **Step 2: 在 ref 声明区增加 `agreementAccepted` 和 `rulesVisible`**

```javascript
const applyVisible = ref(false)
const applyAmount = ref(null)
const applyAccount = ref('')
const rulesVisible = ref(false)
const agreementAccepted = ref(false)
```

- [ ] **Step 3: 增加加载和保存函数**

```javascript
const loadAgreement = () => {
  const raw = localStorage.getItem(WITHDRAW_AGREEMENT_KEY)
  agreementAccepted.value = raw === 'true'
}

const saveAgreement = () => {
  localStorage.setItem(WITHDRAW_AGREEMENT_KEY, String(agreementAccepted.value))
}
```

- [ ] **Step 4: 在 `openApplyModal` 中调用 `loadAgreement`**

```javascript
const openApplyModal = () => {
  if (!realNameVerified.value) {
    message.warning('请先完成实名认证')
    return
  }
  if (coinBalance.value < 100) {
    message.warning('余额不足 100 创作币')
    return
  }
  if (withdrawRecords.value.some((r) => r.status === 'pending')) {
    message.warning('你有正在审核中的提现申请')
    return
  }
  loadAgreement()
  applyAmount.value = null
  applyAccount.value = ''
  applyVisible.value = true
}
```

- [ ] **Step 5: 在 `onMounted` 中调用 `loadAgreement`**

```javascript
onMounted(() => {
  loadRealName()
  loadCoinBalance()
  loadInviteStats()
  loadWithdrawRecords()
  loadAgreement()
})
```

---

### Task 2: 在申请提现弹框中增加协议勾选行

**Files:**
- Modify: `project/user/web/src/views/console/WithdrawIndex.vue`

**Interfaces:**
- Consumes: `agreementAccepted` ref、`saveAgreement()`
- Produces: 无

- [ ] **Step 1: 在 `coin-apply-actions` 上方插入勾选行**

找到如下代码：

```html
        <div class="coin-apply-actions">
          <button class="invite-btn invite-btn-secondary" @click="applyVisible = false">取消</button>
          <button class="invite-btn invite-btn-primary" :disabled="!canSubmitApply" @click="submitApply">提交申请</button>
        </div>
```

在其上方插入：

```html
        <div class="coin-apply-agreement">
          <label class="coin-apply-agreement-label">
            <input v-model="agreementAccepted" type="checkbox" class="coin-apply-agreement-checkbox" @change="saveAgreement" />
            <span>我已阅读并同意</span>
            <span class="coin-apply-agreement-link" @click="rulesVisible = true">《提现服务协议》</span>
          </label>
        </div>
```

- [ ] **Step 2: 修改 `canSubmitApply` 增加协议校验**

```javascript
const canSubmitApply = computed(() => {
  const amount = Number(applyAmount.value)
  if (!amount || amount < 100 || amount > coinBalance.value) return false
  if (!applyAccount.value.trim()) return false
  if (!realNameVerified.value) return false
  if (!agreementAccepted.value) return false
  return true
})
```

- [ ] **Step 3: 在 `submitApply` 开头增加协议提示兜底**

```javascript
const submitApply = () => {
  if (!agreementAccepted.value) {
    message.warning('请先阅读并同意《提现服务协议》')
    return
  }
  if (!canSubmitApply.value) {
    message.warning('请完整填写提现信息')
    return
  }
  // 后续逻辑保持不变
}
```

---

### Task 3: 增加协议全文弹框

**Files:**
- Modify: `project/user/web/src/views/console/WithdrawIndex.vue`

**Interfaces:**
- Consumes: `rulesVisible` ref
- Produces: 无

- [ ] **Step 1: 在申请提现弹框之后增加协议全文弹框**

```html
    <a-modal
      v-model:open="rulesVisible"
      title="提现服务协议"
      :footer="null"
      :width="560"
      centered
    >
      <ol class="coin-rules-list">
        <li>用户承诺提现资金来源合法，不得利用本平台进行洗钱、套现、赌博、诈骗等违法活动。</li>
        <li>用户申请提现的账户信息（支付宝账号、真实姓名）必须与本人实名认证信息一致。</li>
        <li>用户应确保邀请好友行为真实有效，禁止通过虚假注册、刷单、机器刷量等方式获取创作币。</li>
        <li>平台有权对异常提现行为进行审核、延迟到账、拒绝提现或冻结相关收益。</li>
        <li>因用户提供错误账户信息、账户异常或违反法律法规导致的提现失败，平台不承担任何责任。</li>
        <li>平台可根据法律法规及业务需要调整本协议内容，调整后会通过官方渠道通知用户。</li>
      </ol>
      <div class="coin-rules-footer">* 本协议内容仅供参考，具体以平台最终公示为准。</div>
    </a-modal>
```

---

### Task 4: 增加样式

**Files:**
- Modify: `project/user/web/src/views/console/WithdrawIndex.vue`

**Interfaces:**
- Consumes: 无
- Produces: `.coin-apply-agreement`、`.coin-apply-agreement-label`、`.coin-apply-agreement-checkbox`、`.coin-apply-agreement-link`、`.coin-rules-list`、`.coin-rules-footer`

- [ ] **Step 1: 在 `<style scoped>` 末尾追加样式**

```css
.coin-apply-agreement {
  margin-bottom: 18px;
}

.coin-apply-agreement-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
}

.coin-apply-agreement-checkbox {
  width: 14px;
  height: 14px;
  cursor: pointer;
  accent-color: #ff2442;
}

.coin-apply-agreement-link {
  color: #ff2442;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.coin-apply-agreement-link:hover {
  color: #e61e3a;
}

.coin-rules-list {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  color: #595959;
  line-height: 1.8;
}

.coin-rules-list li {
  margin-bottom: 10px;
}

.coin-rules-footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  color: #8c8c8c;
}
```

---

### Task 5: 构建与验证

**Files:**
- 无

**Interfaces:**
- Consumes: 无
- Produces: 无

- [ ] **Step 1: 运行构建**

```bash
cd project/user/web
npm run build
```

预期：构建成功，无错误。

- [ ] **Step 2: 启动开发服务器并截图验证**

```bash
npm run dev -- --port 22347
```

使用 Playwright 访问 `http://localhost:22347/console/coin?from=account`：

1. 点击「申请提现」按钮（需满足余额 ≥ 100 且已完成实名认证）。
2. 确认弹框底部出现「我已阅读并同意《提现服务协议》」勾选行。
3. 确认未勾选时「提交申请」按钮禁用。
4. 勾选后按钮可用。
5. 点击《提现服务协议》打开协议全文弹框。
6. 截图保存到 `/tmp/withdraw_agreement_modal.png`。

---

## Self-Review

**Spec coverage:**
- 强制勾选：Task 2 Step 2 和 Step 3 覆盖。
- 协议全文弹框：Task 3 覆盖。
- 记住同意状态：Task 1 覆盖。
- 反洗钱条款：Task 3 覆盖。

**Placeholder scan：** 无 TBD/TODO，所有代码片段完整。

**Type consistency：** `agreementAccepted` 为 `ref(false)`，与 `canSubmitApply` 中的布尔校验一致。
