<template>
  <div class="plan-list-view">
    <h3 class="page-title">套餐管理</h3>
    <p class="page-desc">
      套餐价格、文章数量、队列任务数、风格市场发布额度、风格学习次数、模板范围及功能开关都在这里统一配置。
      保存后，用户端 <code>/api/v1/user/plans</code> 接口将在 Caffeine 缓存失效（最长 5 分钟）后生效。
    </p>

    <a-card :bordered="false">
      <a-table
        :columns="columns"
        :data-source="plans"
        :loading="loading"
        row-key="planKey"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'planKey'">
            <a-tag :color="record.planKey === 'pro' ? 'red' : 'default'">{{ record.planKey }}</a-tag>
          </template>
          <template v-else-if="column.key === 'recommended'">
            <a-tag v-if="record.recommended === 1" color="red">推荐</a-tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 1"
              :loading="togglingKey === record.planKey"
              checked-children="启用"
              un-checked-children="停用"
              @change="(v) => onToggleStatus(record, v)"
            />
          </template>
          <template v-else-if="column.key === 'priceMonthly'">
            <PriceCell :record="record" field="priceMonthly" :original-field="record.originalMonthly ? 'originalMonthly' : null" />
          </template>
          <template v-else-if="column.key === 'priceQuarter'">
            <PriceCell :record="record" field="priceQuarter" :original-field="record.originalQuarter ? 'originalQuarter' : null" />
          </template>
          <template v-else-if="column.key === 'priceYear'">
            <PriceCell :record="record" field="priceYear" :original-field="record.originalYear ? 'originalYear' : null" />
            <div v-if="record.savingsYear" class="savings">立省 ¥{{ formatPrice(record.savingsYear) }}</div>
          </template>
          <template v-else-if="column.key === 'inviterReward'">
            {{ record.inviterReward ?? 0 }} 创作币
          </template>
          <template v-else-if="column.key === 'action'">
            <a @click="onEdit(record)">编辑</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 套餐基础信息和套餐内容统一在同一抽屉编辑 -->
    <a-drawer
      v-model:open="modalOpen"
      :title="editing ? `编辑套餐 · ${editing.displayName}` : '新增套餐'"
      :width="920"
      :destroy-on-close="false"
    >
      <a-form layout="vertical" :model="form" class="plan-form">
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="套餐 key" required>
                <a-input v-model:value="form.planKey" placeholder="basic / pro / flagship" :disabled="!!editing" />
                <div class="form-hint">保存后不可修改</div>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="显示名" required>
                <a-input v-model:value="form.displayName" placeholder="如：专业版" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="排序号">
                <a-input-number v-model:value="form.sortOrder" :min="0" style="width: 100%" />
                <div class="form-hint">数字越小越靠前</div>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="推荐位">
                <a-switch v-model:checked="form.recommended" checked-children="推荐" un-checked-children="普通" />
                <div class="form-hint">只允许 1 个套餐为推荐</div>
              </a-form-item>
            </a-col>
          </a-row>
        </div>

        <div class="form-section">
          <div class="section-title">月度</div>
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="价格（元）" required>
                <a-input-number v-model:value="form.priceMonthly" :min="0" :precision="2" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="划线价（元）">
                <a-input-number v-model:value="form.originalMonthly" :min="0" :precision="2" style="width: 100%" />
                <div class="form-hint">无折扣可留空</div>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="文章文案">
                <a-input v-model:value="form.articlesMonthly" placeholder="30 篇 AI 文章/月" />
              </a-form-item>
            </a-col>
          </a-row>
        </div>

        <div class="form-section">
          <div class="section-title">季度</div>
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="价格（元）" required>
                <a-input-number v-model:value="form.priceQuarter" :min="0" :precision="2" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="划线价（元）">
                <a-input-number v-model:value="form.originalQuarter" :min="0" :precision="2" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="文章文案">
                <a-input v-model:value="form.articlesQuarter" placeholder="90 篇 AI 文章/季" />
              </a-form-item>
            </a-col>
          </a-row>
        </div>

        <div class="form-section">
          <div class="section-title">年度</div>
          <a-row :gutter="16">
            <a-col :span="6">
              <a-form-item label="价格（元）" required>
                <a-input-number v-model:value="form.priceYear" :min="0" :precision="2" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="划线价（元）">
                <a-input-number v-model:value="form.originalYear" :min="0" :precision="2" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="文章文案">
                <a-input v-model:value="form.articlesYear" placeholder="360 篇 AI 文章/年" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="立省金额">
                <a-input-number v-model:value="form.savingsYear" :min="0" :precision="2" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>
        </div>

        <div class="form-section">
          <div class="section-title">邀请与状态</div>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="邀请奖励（创作币）" required>
                <a-input-number v-model:value="form.inviterReward" :min="0" :precision="2" style="width: 100%" />
                <div class="form-hint">被邀请者开通本套餐时，邀请人获得的创作币</div>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="状态" required>
                <a-radio-group v-model:value="form.status">
                  <a-radio :value="1">启用</a-radio>
                  <a-radio :value="0">停用</a-radio>
                </a-radio-group>
                <div class="form-hint">停用后用户端定价页不再展示</div>
              </a-form-item>
            </a-col>
          </a-row>
        </div>

        <div class="form-section plan-content-section">
          <div class="section-title">套餐内容</div>
          <div class="section-desc">配置该套餐包含的功能、使用额度和服务等级。关闭配额后保存值为 0。</div>
          <a-row :gutter="16">
            <a-col v-for="benefit in benefits" :key="benefit.code" :span="12">
              <div class="benefit-field">
                <div class="benefit-field-header">
                  <span class="benefit-field-name">{{ benefit.displayLabel || benefit.name }}</span>
                  <a-tag :color="benefitTypeColor(benefit.type)">{{ benefitTypeLabel(benefit.type) }}</a-tag>
                </div>

                <template v-if="benefit.type === 'boolean'">
                  <a-switch
                    :checked="benefitValues[benefit.code] === 'true'"
                    checked-children="支持"
                    un-checked-children="不支持"
                    @change="(checked) => setBenefitValue(benefit.code, checked ? 'true' : 'false')"
                  />
                </template>

                <template v-else-if="benefit.type === 'quota'">
                  <div class="quota-editor">
                    <a-switch
                      :checked="isQuotaEnabled(benefit.code)"
                      checked-children="启用"
                      un-checked-children="关闭"
                      @change="(checked) => toggleQuota(benefit.code, checked)"
                    />
                    <a-input-number
                      v-if="isQuotaEnabled(benefit.code)"
                      :value="quotaNumber(benefit.code)"
                      :min="benefit.code === 'history_days' ? -1 : 1"
                      :max="999999"
                      :precision="0"
                      style="width: 150px"
                      @change="(value) => setBenefitValue(benefit.code, String(value ?? 0))"
                    />
                    <span v-else class="disabled-text">该套餐不支持</span>
                  </div>
                  <div v-if="benefit.code === 'history_days' && isQuotaEnabled(benefit.code)" class="form-hint">
                    填 -1 表示永久保留
                  </div>
                </template>

                <template v-else-if="benefit.type === 'tier'">
                  <a-select
                    v-if="parseBenefitOptions(benefit).length"
                    v-model:value="benefitValues[benefit.code]"
                    :options="parseBenefitOptions(benefit)"
                    style="width: 100%"
                  />
                  <a-input v-else v-model:value="benefitValues[benefit.code]" />
                </template>

                <a-input
                  v-else
                  v-model:value="benefitValues[benefit.code]"
                  placeholder="请输入权益值"
                />
                <div class="benefit-field-hint">{{ benefit.description || benefit.code }}</div>
              </div>
            </a-col>
          </a-row>
        </div>
      </a-form>

      <template #footer>
        <a-space>
          <a-button @click="modalOpen = false">取消</a-button>
          <a-button type="primary" :loading="submitting" @click="onSubmit">保存</a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { message } from 'ant-design-vue'
import { fetchPlans, upsertPlan } from '@/api/plan.js'
import { fetchBenefits } from '@/api/benefit.js'
import { fetchPlanBenefits, upsertPlanBenefit } from '@/api/planBenefit.js'

const plans = ref([])
const benefits = ref([])
const planBenefitValueMap = reactive({})
const benefitValues = reactive({})
const originalBenefitValues = reactive({})
const quotaPreviousValues = reactive({})
const loading = ref(false)
const modalOpen = ref(false)
const submitting = ref(false)
const editing = ref(null)
const togglingKey = ref('')

const form = reactive(blankForm())

const columns = [
  { title: 'key', key: 'planKey', width: 100 },
  { title: '套餐名', dataIndex: 'displayName', key: 'displayName', width: 100 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 70 },
  { title: '月度价', key: 'priceMonthly', width: 160 },
  { title: '季度价', key: 'priceQuarter', width: 130 },
  { title: '年度价', key: 'priceYear', width: 180 },
  { title: '推荐', key: 'recommended', width: 80 },
  { title: '邀请奖励', key: 'inviterReward', width: 110 },
  { title: '状态', key: 'status', width: 110 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]

function blankForm() {
  return {
    planKey: '',
    displayName: '',
    sortOrder: 0,
    recommended: false,
    priceMonthly: 0,
    priceQuarter: 0,
    priceYear: 0,
    originalMonthly: null,
    originalQuarter: null,
    originalYear: null,
    articlesMonthly: '',
    articlesQuarter: '',
    articlesYear: '',
    savingsYear: null,
    inviterReward: 0,
    status: 1
  }
}

function formatPrice(value) {
  if (value == null) return '-'
  return Number(value).toFixed(2)
}

/** 表格内"价格 + 划线价"组合单元格 */
const PriceCell = {
  props: ['record', 'field', 'originalField'],
  setup(props) {
    return () => h('div', null, [
      h('span', { class: 'price' }, '¥' + formatPrice(props.record[props.field])),
      props.record[props.originalField]
        ? h('span', { class: 'price-original', style: 'margin-left:6px;' }, '¥' + formatPrice(props.record[props.originalField]))
        : null
    ])
  }
}

function clearReactive(target) {
  Object.keys(target).forEach((key) => delete target[key])
}

function parseBenefitOptions(benefit) {
  if (!benefit.valueLabelJson) return []
  try {
    const labels = JSON.parse(benefit.valueLabelJson)
    return Object.entries(labels).map(([value, label]) => ({ value, label }))
  } catch {
    return []
  }
}

function benefitTypeColor(type) {
  if (type === 'boolean') return 'blue'
  if (type === 'quota') return 'orange'
  return 'purple'
}

function benefitTypeLabel(type) {
  if (type === 'boolean') return '功能开关'
  if (type === 'quota') return '使用额度'
  if (type === 'tier') return '服务等级'
  return '配置项'
}

function defaultBenefitValue(benefit) {
  if (benefit.type === 'boolean') return 'false'
  if (benefit.type === 'quota') return '0'
  return parseBenefitOptions(benefit)[0]?.value || ''
}

function fillBenefitValues(planKey) {
  clearReactive(benefitValues)
  clearReactive(originalBenefitValues)
  clearReactive(quotaPreviousValues)
  benefits.value.forEach((benefit) => {
    const value = planBenefitValueMap[`${planKey}:${benefit.code}`] ?? defaultBenefitValue(benefit)
    benefitValues[benefit.code] = String(value)
    originalBenefitValues[benefit.code] = String(value)
    if (benefit.type === 'quota' && Number(value) !== 0) {
      quotaPreviousValues[benefit.code] = String(value)
    }
  })
}

function setBenefitValue(code, value) {
  benefitValues[code] = value
  if (Number(value) !== 0) quotaPreviousValues[code] = value
}

function quotaNumber(code) {
  const value = Number(benefitValues[code])
  return Number.isFinite(value) ? value : 0
}

function isQuotaEnabled(code) {
  return quotaNumber(code) !== 0
}

function toggleQuota(code, checked) {
  if (!checked) {
    if (isQuotaEnabled(code)) quotaPreviousValues[code] = benefitValues[code]
    benefitValues[code] = '0'
    return
  }
  benefitValues[code] = quotaPreviousValues[code] || '1'
}

async function load() {
  loading.value = true
  try {
    const [planList, benefitList, planBenefitList] = await Promise.all([
      fetchPlans(),
      fetchBenefits(),
      fetchPlanBenefits()
    ])
    plans.value = planList
    benefits.value = [...benefitList].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    clearReactive(planBenefitValueMap)
    planBenefitList.forEach((item) => {
      planBenefitValueMap[`${item.planKey}:${item.benefitCode}`] = String(item.benefitValue)
    })
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onEdit(record) {
  editing.value = record
  Object.assign(form, blankForm(), {
    planKey: record.planKey,
    displayName: record.displayName,
    sortOrder: record.sortOrder || 0,
    recommended: record.recommended === 1,
    priceMonthly: Number(record.priceMonthly || 0),
    priceQuarter: Number(record.priceQuarter || 0),
    priceYear: Number(record.priceYear || 0),
    originalMonthly: record.originalMonthly != null ? Number(record.originalMonthly) : null,
    originalQuarter: record.originalQuarter != null ? Number(record.originalQuarter) : null,
    originalYear: record.originalYear != null ? Number(record.originalYear) : null,
    articlesMonthly: record.articlesMonthly || '',
    articlesQuarter: record.articlesQuarter || '',
    articlesYear: record.articlesYear || '',
    savingsYear: record.savingsYear != null ? Number(record.savingsYear) : null,
    inviterReward: Number(record.inviterReward || 0),
    status: record.status ?? 1
  })
  fillBenefitValues(record.planKey)
  modalOpen.value = true
}

async function onToggleStatus(record, checked) {
  togglingKey.value = record.planKey
  try {
    await upsertPlan({
      ...record,
      priceMonthly: Number(record.priceMonthly),
      priceQuarter: Number(record.priceQuarter),
      priceYear: Number(record.priceYear),
      inviterReward: Number(record.inviterReward),
      status: checked ? 1 : 0
    })
    message.success(checked ? '已启用' : '已停用')
    await load()
  } catch (e) {
    message.error(e?.message || '切换失败')
  } finally {
    togglingKey.value = ''
  }
}

async function onSubmit() {
  if (!form.planKey.trim()) { message.error('套餐 key 不能为空'); return }
  if (!form.displayName.trim()) { message.error('显示名不能为空'); return }
  if (form.priceMonthly < 0 || form.priceQuarter < 0 || form.priceYear < 0) {
    message.error('价格不能为负'); return
  }
  submitting.value = true
  try {
    const planKey = form.planKey.trim()
    await upsertPlan({
      ...form,
      planKey,
      recommended: form.recommended ? 1 : 0,
      status: Number(form.status)
    })

    const changedBenefits = benefits.value.filter(
      (benefit) => benefitValues[benefit.code] !== originalBenefitValues[benefit.code]
    )
    await Promise.all(changedBenefits.map((benefit) => upsertPlanBenefit({
      planKey,
      benefitCode: benefit.code,
      benefitValue: benefitValues[benefit.code]
    })))

    message.success('套餐配置已保存')
    modalOpen.value = false
    await load()
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.plan-list-view { padding: 0; }
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}
.page-desc {
  color: #8c8c8c;
  margin: 0 0 16px;
  font-size: 13px;
}
.page-desc code {
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12px;
}
.price {
  font-weight: 600;
  color: #1a1a1a;
}
.price-original {
  color: #bfbfbf;
  text-decoration: line-through;
  font-size: 12px;
}
.savings {
  font-size: 12px;
  color: #ff2442;
  margin-top: 2px;
}
.plan-form { padding-top: 8px; }
.form-section {
  margin-bottom: 20px;
  padding-bottom: 4px;
  border-bottom: 1px dashed #f0f0f0;
}
.form-section:last-child { border-bottom: none; }
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #ff2442;
}
.form-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}
.section-desc {
  margin: -4px 0 14px 11px;
  color: #8c8c8c;
  font-size: 12px;
}
.plan-content-section {
  padding-bottom: 0;
}
.benefit-field {
  min-height: 126px;
  margin-bottom: 16px;
  padding: 14px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}
.benefit-field-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}
.benefit-field-name {
  color: #262626;
  font-size: 14px;
  font-weight: 600;
}
.quota-editor {
  display: flex;
  align-items: center;
  gap: 10px;
}
.disabled-text {
  color: #bfbfbf;
  font-size: 12px;
}
.benefit-field-hint {
  margin-top: 8px;
  color: #8c8c8c;
  font-size: 11px;
  line-height: 1.5;
}
</style>