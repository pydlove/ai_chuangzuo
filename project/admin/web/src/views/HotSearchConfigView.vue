<template>
  <div class="hot-search-config">
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="14">
        <a-card title="抓取配置">
          <a-form layout="vertical" :model="form" :rules="rules" ref="formRef">
            <a-form-item label="cron 表达式（如 0 0 2 * * ?）" name="cron">
              <a-input v-model:value="form.cron" placeholder="0 0 2 * * ?" />
            </a-form-item>
            <a-form-item label="启用定时抓取" name="enabled">
              <a-switch v-model:checked="enabledBool" />
            </a-form-item>
            <a-form-item label="每个平台前 N 条" name="topN">
              <a-input-number v-model:value="form.topN" :min="1" :max="200" />
            </a-form-item>
            <a-form-item label="连接超时 (ms)" name="connectTimeoutMillis">
              <a-input-number v-model:value="form.connectTimeoutMillis" :min="100" />
            </a-form-item>
            <a-form-item label="读取超时 (ms)" name="readTimeoutMillis">
              <a-input-number v-model:value="form.readTimeoutMillis" :min="100" />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="handleSave">保存配置</a-button>
                <a-button @click="handleCrawlNow" :loading="crawling">立即抓取一次</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="10">
        <a-card title="上次抓取摘要">
          <template v-if="state.lastRun.lastRunAt">
            <p>抓取时间：{{ formatTime(state.lastRun.lastRunAt) }}</p>
            <p>总条数：{{ state.lastRun.totalFetched }}</p>
            <p>成功：<a-tag color="green">{{ state.lastRun.successCount }}</a-tag> 失败：<a-tag color="red">{{ state.lastRun.failCount }}</a-tag></p>
            <a-divider />
            <a-list
              :data-source="state.lastRun.results"
              size="small"
              :pagination="{ pageSize: 5 }"
            >
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-space>
                    <span>{{ item.platformName || item.platformCode }}</span>
                    <a-tag :color="item.success ? 'green' : 'red'">
                      {{ item.success ? '成功' : '失败' }}
                    </a-tag>
                    <span v-if="item.success">{{ item.fetched }} 条</span>
                    <span v-else style="color:#cf1322">{{ item.error }}</span>
                  </a-space>
                </a-list-item>
              </template>
            </a-list>
          </template>
          <a-empty v-else description="暂无抓取记录" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useHotSearch } from '@/composables/useHotSearch.js'

const { state, fetchConfig, fetchLastRun, saveConfig, crawlNow } = useHotSearch()

const form = reactive({ cron: '', enabled: 1, topN: 50, connectTimeoutMillis: 5000, readTimeoutMillis: 10000 })
const enabledBool = computed({ get: () => form.enabled === 1, set: (v) => (form.enabled = v ? 1 : 0) })
const formRef = ref()
const rules = {
  cron: [{ required: true, message: '请输入 cron 表达式' }],
  topN: [{ required: true, message: '请输入条数' }]
}

const formatTime = (s) => new Date(s).toLocaleString()

const handleSave = async () => {
  await formRef.value?.validate()
  await saveConfig({ ...form })
  await fetchConfig()
}
const crawling = ref(false)
const handleCrawlNow = async () => {
  crawling.value = true
  try {
    await crawlNow()
    await fetchLastRun()
  } finally {
    crawling.value = false
  }
}

onMounted(async () => {
  await fetchConfig()
  Object.assign(form, state.config)
  await fetchLastRun()
})
</script>

<style scoped>.hot-search-config { padding: 0; }</style>
