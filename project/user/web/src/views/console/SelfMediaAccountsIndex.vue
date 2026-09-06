<template>
  <div class="sma-page">
    <!-- 移动端 Hero（参考「我的提示词」页） -->
    <MobileConsoleHero
      title="自媒体账号"
      desc="添加你在各平台的自媒体账号，创作分发更顺畅。"
      image-url="/assets/images/自媒体账号icon-v1.png"
    />

    <!-- PC 端页面标题与操作 -->
    <header class="sma-page-header">
      <div class="sma-page-title-wrap">
        <h1 class="sma-page-title">自媒体账号</h1>
        <p class="sma-page-subtitle">管理你在各平台的自媒体账号，创作分发更顺畅</p>
      </div>
      <button class="sma-page__action" @click="openCreate">
        <PlusOutlined />
        新增账号
      </button>
    </header>

    <!-- 移动端新增入口 -->
    <div class="sma-mobile-add">
      <button class="sma-mobile-add-btn" @click="openCreate">
        <PlusOutlined />
        新增账号
      </button>
    </div>

    <div class="sma-page-body">
      <!-- 桌面端：表格 -->
      <div class="sma-desktop-table">
        <a-table
          :columns="columns"
          :data-source="accounts"
          :row-key="record => record.id"
          :pagination="false"
          :loading="loading"
          :locale="tableLocale"
          :scroll="{ x: 720 }"
          class="sma-table"
        />
      </div>

      <!-- 移动端：卡片列表 -->
      <div v-show="accounts.length > 0" class="sma-mobile-list">
        <div v-for="acc in accounts" :key="acc.id" class="sma-card">
          <div class="sma-card__header">
            <span class="sma-platform">
              <span
                class="sma-platform__badge"
                :style="{ background: platformColor(acc.platform) }"
              >{{ platformLabel(acc.platform).charAt(0) }}</span>
              <span class="sma-platform__name">{{ platformLabel(acc.platform) }}</span>
            </span>
          </div>
          <div class="sma-card__rows">
            <div class="sma-card__row">
              <span class="sma-card__label">账号昵称</span>
              <span class="sma-card__value">{{ acc.accountName }}</span>
            </div>
            <div v-if="acc.accountId" class="sma-card__row">
              <span class="sma-card__label">账号ID</span>
              <span class="sma-card__value">{{ acc.accountId }}</span>
            </div>
            <div v-if="acc.homeUrl" class="sma-card__row">
              <span class="sma-card__label">主页链接</span>
              <span class="sma-card__value sma-card__value--link">{{ acc.homeUrl }}</span>
            </div>
          </div>
          <div class="sma-card__footer">
            <button class="sma-card__btn" @click="openEdit(acc)">编辑</button>
            <button class="sma-card__btn sma-card__btn--danger" @click="removeAccount(acc)">删除</button>
          </div>
        </div>
      </div>

      <EmptyState
        v-if="accounts.length === 0 && !loading"
        :icon="IdcardOutlined"
        title="暂无自媒体账号"
        description="添加你在公众号、小红书、抖音等平台的账号，创作分发更顺畅"
        action-text="新增账号"
        size="lg"
        @action="openCreate"
      />
    </div>

    <!-- 新增/编辑弹框 -->
    <a-modal
      v-model:open="formVisible"
      :title="editingId ? '编辑账号' : '新增账号'"
      :width="modalWidth"
      centered
      ok-text="保存"
      cancel-text="取消"
      class="sma-form-modal"
      @ok="saveAccount"
    >
      <div class="sma-form">
        <div class="sma-form__field">
          <label class="sma-form__label">平台 <span class="sma-form__required">*</span></label>
          <a-select
            v-model:value="form.platform"
            class="sma-form__control"
            placeholder="选择平台"
            popup-class-name="sma-form-select-dropdown"
          >
            <a-select-option
              v-for="p in PLATFORM_OPTIONS"
              :key="p.key"
              :value="p.key"
            >{{ p.label }}</a-select-option>
          </a-select>
        </div>
        <div class="sma-form__field">
          <label class="sma-form__label">账号昵称 <span class="sma-form__required">*</span></label>
          <a-input
            v-model:value="form.accountName"
            class="sma-form__control"
            placeholder="例如：爱创作的小爱"
            :maxlength="30"
          />
        </div>
        <div class="sma-form__field">
          <label class="sma-form__label">账号ID</label>
          <a-input
            v-model:value="form.accountId"
            class="sma-form__control"
            placeholder="选填，例如抖音号、公众号ID"
            :maxlength="50"
          />
        </div>
        <div class="sma-form__field">
          <label class="sma-form__label">主页链接</label>
          <a-input
            v-model:value="form.homeUrl"
            class="sma-form__control"
            placeholder="选填，账号主页链接"
            :maxlength="200"
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, IdcardOutlined } from '@ant-design/icons-vue'
import MobileConsoleHero from '@/components/MobileConsoleHero.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { PLATFORM_OPTIONS, PLATFORM_NAME_MAP } from '@/utils/platform.js'
import { STORAGE_KEYS } from '@/constants/storage.js'

const accounts = ref([])
const loading = ref(false)
const formVisible = ref(false)
const editingId = ref(null)
const form = ref(emptyForm())

const modalWidth = ref(360)

function emptyForm() {
  return { platform: undefined, accountName: '', accountId: '', homeUrl: '' }
}

function updateModalWidth() {
  modalWidth.value = window.innerWidth >= 769 ? 480 : 360
}

// 平台徽标配色（浅底小圆点，避免大面积色块）
const PLATFORM_COLORS = {
  wechat: '#07c160',
  xiaohongshu: '#ff2442',
  toutiao: '#f04142',
  baijiahao: '#306eff',
  douyin: '#fe2c55',
  kuaishou: '#ff6f06',
  zhihu: '#0066ff',
  bilibili: '#fb7299'
}

function platformLabel(key) {
  return PLATFORM_NAME_MAP[key] || key || '-'
}

function platformColor(key) {
  return PLATFORM_COLORS[key] || '#8c8c8c'
}

function platformCell(record) {
  return h('span', { class: 'sma-platform' }, [
    h('span', {
      class: 'sma-platform__badge',
      style: { background: platformColor(record.platform) }
    }, platformLabel(record.platform).charAt(0)),
    h('span', { class: 'sma-platform__name' }, platformLabel(record.platform))
  ])
}

function actionCell(record) {
  return h('span', { class: 'sma-table__actions' }, [
    h('button', { class: 'sma-table__action', onClick: () => openEdit(record) }, '编辑'),
    h('button', { class: 'sma-table__action sma-table__action--danger', onClick: () => removeAccount(record) }, '删除')
  ])
}

const columns = [
  { title: '平台', key: 'platform', width: 140, customRender: ({ record }) => platformCell(record) },
  { title: '账号昵称', dataIndex: 'accountName', key: 'accountName' },
  { title: '账号ID', dataIndex: 'accountId', key: 'accountId', customRender: ({ text }) => text || '-' },
  { title: '主页链接', dataIndex: 'homeUrl', key: 'homeUrl', customRender: ({ text }) => text || '-' },
  { title: '操作', key: 'action', width: 110, customRender: ({ record }) => actionCell(record) }
]

const tableLocale = computed(() => ({
  emptyText: h('div', { class: 'sma-table-empty' }, [
    h('div', { class: 'sma-table-empty__text' }, '暂无自媒体账号'),
    h('div', { class: 'sma-table-empty__desc' }, '点击右上角「新增账号」，添加你在各平台的自媒体账号')
  ])
}))

function loadAccounts() {
  loading.value = true
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.SELF_MEDIA_ACCOUNTS)
    accounts.value = raw ? JSON.parse(raw) : []
  } catch {
    accounts.value = []
  } finally {
    loading.value = false
  }
}

function persistAccounts() {
  localStorage.setItem(STORAGE_KEYS.SELF_MEDIA_ACCOUNTS, JSON.stringify(accounts.value))
}

function openCreate() {
  editingId.value = null
  form.value = emptyForm()
  formVisible.value = true
}

function openEdit(acc) {
  editingId.value = acc.id
  form.value = { platform: acc.platform, accountName: acc.accountName, accountId: acc.accountId || '', homeUrl: acc.homeUrl || '' }
  formVisible.value = true
}

function saveAccount() {
  const { platform, accountName, accountId, homeUrl } = form.value
  if (!platform) {
    message.warning('请选择平台')
    return
  }
  if (!accountName || !accountName.trim()) {
    message.warning('请填写账号昵称')
    return
  }
  const payload = {
    platform,
    accountName: accountName.trim(),
    accountId: accountId.trim(),
    homeUrl: homeUrl.trim()
  }
  if (editingId.value) {
    const idx = accounts.value.findIndex(a => a.id === editingId.value)
    if (idx > -1) accounts.value.splice(idx, 1, { ...accounts.value[idx], ...payload })
    message.success('账号已更新')
  } else {
    accounts.value.push({ id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`, ...payload })
    message.success('账号已添加')
  }
  persistAccounts()
  formVisible.value = false
}

function removeAccount(acc) {
  Modal.confirm({
    title: '删除账号',
    content: `确定删除「${platformLabel(acc.platform)} · ${acc.accountName}」吗？`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    centered: true,
    onOk: () => {
      accounts.value = accounts.value.filter(a => a.id !== acc.id)
      persistAccounts()
      message.success('账号已删除')
    }
  })
}

onMounted(() => {
  loadAccounts()
  updateModalWidth()
  window.addEventListener('resize', updateModalWidth)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateModalWidth)
})
</script>

<style scoped>
.sma-page {
  min-height: 100%;
  background: #f5f6fa;
  padding: 12px 12px calc(24px + env(safe-area-inset-bottom));
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  box-sizing: border-box;
}

/* 页面标题：PC 端显示 */
.sma-page-header {
  display: none;
}

.sma-page-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sma-page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.sma-page-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.sma-page__action {
  display: none;
}

/* 移动端新增入口 */
.sma-mobile-add {
  margin-bottom: 12px;
}

.sma-mobile-add-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 13px 16px;
  border: 1px dashed #ffb3c0;
  border-radius: 14px;
  background: #fff;
  color: #ff2442;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.sma-mobile-add-btn:active {
  background: #fff0f2;
}

/* 平台徽标（PC 表格 h() 渲染，需 :global 才能命中；移动卡片也复用） */
:global(.sma-platform) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

:global(.sma-platform__badge) {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
}

:global(.sma-platform__name) {
  font-size: 14px;
  color: #1a1a1a;
  font-weight: 500;
}

/* 移动端卡片列表 */
.sma-mobile-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sma-desktop-table {
  display: none;
}

.sma-card {
  background: #fff;
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.sma-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #f5f5f5;
}

.sma-card__rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 0;
}

.sma-card__row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  font-size: 13px;
}

.sma-card__label {
  flex-shrink: 0;
  width: 60px;
  color: #999;
}

.sma-card__value {
  flex: 1;
  min-width: 0;
  color: #1a1a1a;
  word-break: break-all;
}

.sma-card__value--link {
  color: #1677ff;
}

.sma-card__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}

.sma-card__btn {
  padding: 5px 14px;
  border-radius: 14px;
  border: 1px solid #e8e8e8;
  background: #fff;
  color: #595959;
  font-size: 12px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.sma-card__btn:active {
  background: #f5f5f5;
}

.sma-card__btn--danger {
  color: #ff4d4f;
  border-color: #ffd6d6;
}

/* 表单弹框 */
.sma-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px 0 8px;
}

.sma-form__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sma-form__label {
  font-size: 13px;
  color: #1a1a1a;
  font-weight: 500;
}

.sma-form__required {
  color: #ff4d4f;
}

.sma-form__control {
  width: 100%;
}

/* 表格操作按钮（h() 渲染，需 :global） */
:global(.sma-table__actions) {
  display: inline-flex;
  gap: 12px;
}

:global(.sma-table__action) {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 13px;
  color: #1677ff;
  cursor: pointer;
}

:global(.sma-table__action--danger) {
  color: #ff4d4f;
}

:global(.sma-table-empty) {
  padding: 32px 0;
}

:global(.sma-table-empty__text) {
  font-size: 14px;
  color: #595959;
  margin-bottom: 6px;
}

:global(.sma-table-empty__desc) {
  font-size: 12px;
  color: #999;
}

/* ===== PC 端 ===== */
@media (min-width: 769px) {
  .sma-page {
    background: transparent;
    padding: 24px 32px calc(32px + env(safe-area-inset-bottom));
  }

  .sma-page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 8px;
  }

  .sma-page__action {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 38px;
    padding: 0 18px;
    border: none;
    border-radius: 19px;
    background: linear-gradient(135deg, #ff6b7d 0%, #ff2442 100%);
    color: #fff;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(255, 36, 66, 0.25);
    transition: opacity 0.15s;
  }

  .sma-page__action:hover {
    opacity: 0.9;
  }

  .sma-mobile-add {
    display: none;
  }

  .sma-desktop-table {
    display: block;
    overflow-x: auto;
  }

  .sma-mobile-list {
    display: none;
  }

  .sma-page-body {
    background: #fff;
    border-radius: 14px;
    padding: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

/* ===== 暗色主题 ===== */
body[data-theme="dark"] .sma-page {
  background: #141414;
}

body[data-theme="dark"] .sma-page-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .sma-page-subtitle {
  color: #737373;
}

body[data-theme="dark"] .sma-mobile-add-btn {
  background: #1f1f1f;
  border-color: rgba(255, 36, 66, 0.35);
  color: #ff4d6d;
}

body[data-theme="dark"] .sma-mobile-add-btn:active {
  background: #2a2a2a;
}

body[data-theme="dark"] .sma-card,
body[data-theme="dark"] .sma-page-body {
  background: #1f1f1f;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .sma-card__header,
body[data-theme="dark"] .sma-card__footer {
  border-color: #2a2a2a;
}

:global(body[data-theme="dark"] .sma-platform__name) {
  color: #f0f0f0;
}

body[data-theme="dark"] .sma-card__value {
  color: #f0f0f0;
}

body[data-theme="dark"] .sma-card__label {
  color: #737373;
}

body[data-theme="dark"] .sma-card__btn {
  background: #2a2a2a;
  border-color: #3a3a3a;
  color: #a6a6a6;
}

body[data-theme="dark"] .sma-card__btn--danger {
  color: #ff7875;
  border-color: rgba(255, 77, 79, 0.3);
}

/* ===== 弹框控件统一主题色（#FF2442） ===== */
:global(.sma-form-modal .ant-btn-primary) {
  background: #ff2442;
  border-color: #ff2442;
  box-shadow: 0 2px 6px rgba(255, 36, 66, 0.25);
}

:global(.sma-form-modal .ant-btn-primary:hover),
:global(.sma-form-modal .ant-btn-primary:focus) {
  background: #ff4d6d;
  border-color: #ff4d6d;
}

:global(.sma-form-modal .ant-input:hover),
:global(.sma-form-modal .ant-input:focus),
:global(.sma-form-modal .ant-input-focused) {
  border-color: #ff2442;
}

:global(.sma-form-modal .ant-input:focus),
:global(.sma-form-modal .ant-input-focused) {
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.1);
}

:global(.sma-form-modal .ant-select:not(.ant-select-disabled):hover .ant-select-selector),
:global(.sma-form-modal .ant-select-focused .ant-select-selector),
:global(.sma-form-modal .ant-select-open .ant-select-selector) {
  border-color: #ff2442 !important;
}

:global(.sma-form-modal .ant-select-focused .ant-select-selector) {
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.1) !important;
}

:global(.sma-form-select-dropdown .ant-select-item-option-selected:not(.ant-select-item-option-disabled)) {
  background: #fff0f2;
  color: #ff2442;
  font-weight: 600;
}

:global(.sma-form-modal .ant-modal-close:hover) {
  color: #ff2442;
}

body[data-theme="dark"] :global(.sma-form-select-dropdown .ant-select-item-option-selected:not(.ant-select-item-option-disabled)) {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6d;
}
</style>
