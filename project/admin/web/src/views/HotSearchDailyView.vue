<template>
  <div class="hot-search-daily">
    <a-card title="每日榜单管理">
      <a-form layout="inline" :model="query" class="filter-bar">
        <a-form-item label="平台">
          <a-select v-model:value="query.platform" allow-clear style="width: 160px" placeholder="全部">
            <a-select-option v-for="p in state.platforms" :key="p.code" :value="p.code">
              {{ p.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="日期">
          <a-date-picker v-model:value="dateObj" value-format="YYYY-MM-DD" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="reload">查询</a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="openCreate">新增条目</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :data-source="state.daily.items"
        :columns="columns"
        :loading="state.loading"
        row-key="id"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确认删除？" @confirm="handleDelete(record.id)">
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
              <a-popconfirm title="将重新抓取该平台当日数据，确认？" @confirm="handleRecrawl(record)">
                <a-button size="small">重抓</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" :title="editing.id ? '编辑条目' : '新增条目'" @ok="handleSubmit">
      <a-form layout="vertical" :model="editing">
        <a-form-item label="平台" :required="true">
          <a-select v-model:value="editing.platformCode" :disabled="!!editing.id">
            <a-select-option v-for="p in state.platforms" :key="p.code" :value="p.code">
              {{ p.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="排名" :required="true">
          <a-input-number v-model:value="editing.rankNum" :min="1" />
        </a-form-item>
        <a-form-item label="标题" :required="true">
          <a-input v-model:value="editing.title" />
        </a-form-item>
        <a-form-item label="热度值">
          <a-input v-model:value="editing.hotValue" />
        </a-form-item>
        <a-form-item label="URL">
          <a-input v-model:value="editing.url" />
        </a-form-item>
        <a-form-item label="搜索量">
          <a-input-number v-model:value="editing.searchCount" :min="0" />
        </a-form-item>
        <a-form-item label="快照日期" :required="true">
          <a-date-picker v-model:value="editing.snapshotDate" value-format="YYYY-MM-DD" style="width:100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useHotSearch } from '@/composables/useHotSearch.js'

const { state, fetchPlatforms, fetchDaily, saveDaily, updateDaily, removeDaily, recrawlDaily } = useHotSearch()

const columns = [
  { title: '日期', dataIndex: 'snapshotDate', key: 'snapshotDate', width: 120 },
  { title: '平台', dataIndex: 'platformName', key: 'platformName', width: 120 },
  { title: '排名', dataIndex: 'rankNum', key: 'rankNum', width: 80 },
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '热度', dataIndex: 'hotValue', key: 'hotValue', width: 100 },
  { title: '操作', key: 'action', width: 240 }
]

const query = reactive({ platform: '', date: '', page: 1, size: 20 })
const dateObj = ref('')
watch(dateObj, (v) => (query.date = v || ''))

const pagination = computed(() => ({
  current: Number(state.daily.page) || 1,
  pageSize: Number(state.daily.size) || 20,
  total: Number(state.daily.total) || 0,
  showSizeChanger: true
}))

const reload = () => fetchDaily({ ...query })
const handleTableChange = (p) => {
  query.page = p.current
  query.size = p.pageSize
  fetchDaily({ ...query })
}

const modalOpen = ref(false)
const editing = reactive({
  id: null, platformCode: '', rankNum: 1, title: '', hotValue: '', url: '', searchCount: null, snapshotDate: ''
})
const openCreate = () => {
  editing.id = null
  editing.platformCode = state.platforms[0]?.code || ''
  editing.rankNum = 1
  editing.title = ''
  editing.hotValue = ''
  editing.url = ''
  editing.searchCount = null
  editing.snapshotDate = new Date().toISOString().slice(0, 10)
  modalOpen.value = true
}
const openEdit = (r) => Object.assign(editing, r)
const handleSubmit = async () => {
  if (!editing.platformCode || !editing.title || !editing.snapshotDate) {
    message.warning('请填写平台、标题、日期')
    return
  }
  const payload = { ...editing }
  delete payload.id
  if (editing.id) await updateDaily(editing.id, payload)
  else await saveDaily(payload)
  modalOpen.value = false
  reload()
}
const handleDelete = async (id) => { await removeDaily(id); reload() }
const handleRecrawl = async (record) => { await recrawlDaily(record.id); reload() }

onMounted(async () => {
  await fetchPlatforms()
  await reload()
})
</script>

<style scoped>
.hot-search-daily .filter-bar { margin-bottom: 16px; }
</style>
