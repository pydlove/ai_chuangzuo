<template>
  <div class="user-list">
    <a-card :bordered="false" class="user-list-card">
      <div class="user-list-header">
        <h3 class="user-list-title">注册用户管理</h3>
        <p class="user-list-desc">查看与管理平台注册用户</p>
      </div>

      <!-- 工具栏 -->
      <div class="user-list-toolbar">
        <a-input
          v-model:value="keyword"
          placeholder="账号或邮箱"
          allow-clear
          style="width: 200px"
          @press-enter="handleSearch"
        />
        <a-input
          v-model:value="inviteCode"
          placeholder="邀请码"
          allow-clear
          style="width: 160px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button @click="fetchUsers">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button type="primary" @click="openCreateModal">
          <template #icon><PlusOutlined /></template>
          手动创建
        </a-button>
        <a-button v-if="selectedRowKeys.length > 0" danger @click="confirmBatchDelete">
          <template #icon><DeleteOutlined /></template>
          批量删除 ({{ selectedRowKeys.length }})
        </a-button>
        <a-button @click="downloadTemplate">
          <template #icon><DownloadOutlined /></template>
          下载导入模板
        </a-button>
        <a-upload
          accept=".xlsx"
          :show-upload-list="false"
          :before-upload="beforeImport"
          :custom-request="handleImport"
        >
          <a-button :loading="importing">
            <template #icon><UploadOutlined /></template>
            导入用户
          </a-button>
        </a-upload>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="users"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 'max-content' }"
        row-key="id"
        size="middle"
        :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'email'">
            <span>{{ record.email }}</span>
            <a-button type="link" size="small" class="email-copy-btn" @click="copyEmail(record.email)">
              <template #icon><CopyOutlined /></template>
            </a-button>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'enabled' ? 'green' : 'red'">
              {{ record.status === 'enabled' ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'userType'">
            <a-tag :color="record.userType === 'robot' ? 'orange' : 'blue'">
              {{ record.userType === 'robot' ? '机器人' : '真实用户' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'inviteCode'">
            <span v-if="record.inviteCode">{{ record.inviteCode }}</span>
            <span v-else style="color: #8c8c8c">—</span>
            <a-button
              v-if="record.inviteCode"
              type="link"
              size="small"
              class="invite-code-copy-btn"
              @click="copyInviteCode(record.inviteCode)"
            >
              <template #icon><CopyOutlined /></template>
            </a-button>
          </template>
          <template v-else-if="column.key === 'inviter'">
            <span v-if="record.inviterEmail">
              {{ record.inviterNickname || record.inviterEmail }}
              <span style="color: #8c8c8c; font-size: 12px">({{ record.inviterEmail }})</span>
            </span>
            <span v-else style="color: #8c8c8c">—</span>
          </template>
          <template v-else-if="column.key === 'invitedCount'">
            <a-button
              v-if="record.invitedCount > 0"
              type="link"
              size="small"
              @click="openInviteModal(record)"
            >
              {{ record.invitedCount }}
            </a-button>
            <span v-else style="color: #8c8c8c">0</span>
          </template>
          <template v-else-if="column.key === 'membershipExpireAt'">
            <span v-if="record.membershipExpireAt">{{ formatDateTime(record.membershipExpireAt) }}</span>
            <span v-else style="color: #8c8c8c">非会员</span>
          </template>
          <template v-else-if="column.key === 'membershipPlan'">
            <span v-if="record.membershipPlan">{{ planLabel(record.membershipPlan) }}</span>
            <span v-else style="color: #8c8c8c">—</span>
          </template>
          <template v-else-if="column.key === 'lastLoginAt'">
            {{ record.lastLoginAt || '—' }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-dropdown>
              <a-button size="small">
                设置
                <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="confirmStatusChange(record)">
                    {{ record.status === 'enabled' ? '禁用' : '启用' }}
                  </a-menu-item>
                  <a-menu-item @click="openEditModal(record)">编辑</a-menu-item>
                  <a-menu-item @click="openResetPasswordModal(record)">重置密码</a-menu-item>
                  <a-menu-item @click="openInviteModal(record)">邀请关系</a-menu-item>
                  <a-menu-item @click="openDetailDrawer(record)">查看详情</a-menu-item>
                  <a-menu-item danger @click="confirmDelete(record)">删除</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
        </template>
      </a-table>

      <!-- 分页 -->
      <div class="user-list-pagination">
        <a-pagination
          :current="page"
          :page-size="pageSize"
          :total="total"
          :page-size-options="['10', '20', '50']"
          show-size-changer
          show-total
          @change="handlePageChange"
          @show-size-change="handlePageChange"
        />
      </div>
    </a-card>

    <!-- 重置密码弹框 -->
    <a-modal
      v-model:open="resetPasswordVisible"
      title="重置用户密码"
      ok-text="确认重置"
      cancel-text="取消"
      @ok="confirmResetPassword"
    >
      <p>账号：<strong>{{ resetPasswordTarget?.account }}</strong></p>
      <p style="color: #8c8c8c; margin-top: 12px">
        重置后密码将设为 <code>Aichuangzuo@123</code>，请通知用户及时修改。
      </p>
    </a-modal>

    <!-- 编辑用户弹框 -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑用户"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="editLoading"
      @ok="submitEditForm"
      @cancel="closeEditModal"
    >
      <a-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        layout="vertical"
      >
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="editForm.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="editForm.nickname" placeholder="请输入昵称" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="editForm.status">
            <a-radio value="enabled">启用</a-radio>
            <a-radio value="disabled">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="用户类型" name="userType">
          <a-radio-group v-model:value="editForm.userType">
            <a-radio :value="1">真实用户</a-radio>
            <a-radio :value="0">机器人</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="会员套餐" name="membershipPlan">
          <a-select v-model:value="editForm.membershipPlan" allow-clear placeholder="选择会员套餐（清空=无套餐）" style="width: 100%">
            <a-select-option v-for="plan in plans" :key="plan.planKey" :value="plan.planKey">
              {{ plan.displayName || plan.planKey }}<span v-if="plan.status === 0" style="color: #8c8c8c">（已停用）</span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="会员到期" name="expireDate">
          <a-date-picker
            v-model:value="editForm.expireDate"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            placeholder="选择到期日（清空=非会员）"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 查看详情弹框 -->
    <a-modal
      v-model:open="detailVisible"
      title="用户详情"
      width="100%"
      :style="{ maxWidth: '1280px' }"
      class="user-detail-modal"
      :footer="null"
      @cancel="closeDetailModal"
    >
      <a-tabs v-model:activeKey="detailTabKey" type="card">
        <a-tab-pane key="basic" tab="基础信息">
          <a-descriptions v-if="detailUser" :column="1" bordered>
            <a-descriptions-item label="ID">{{ detailUser.id }}</a-descriptions-item>
            <a-descriptions-item label="邮箱/账号">{{ detailUser.email }}</a-descriptions-item>
            <a-descriptions-item label="昵称">{{ detailUser.nickname }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="detailUser.status === 'enabled' ? 'green' : 'red'">
                {{ detailUser.status === 'enabled' ? '启用' : '禁用' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="类型">
              <a-tag :color="detailUser.userType === 'robot' ? 'orange' : 'blue'">
                {{ detailUser.userType === 'robot' ? '机器人' : '真实用户' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="邀请码">{{ detailUser.inviteCode }}</a-descriptions-item>
            <a-descriptions-item label="会员套餐">
              <span v-if="detailUser.membershipPlan">{{ planLabel(detailUser.membershipPlan) }}</span>
              <span v-else>—</span>
            </a-descriptions-item>
            <a-descriptions-item label="会员到期">
              <span v-if="detailUser.membershipExpireAt">{{ formatDateTime(detailUser.membershipExpireAt) }}</span>
              <span v-else>非会员</span>
            </a-descriptions-item>
            <a-descriptions-item label="注册时间">{{ formatDateTime(detailUser.createdAt) }}</a-descriptions-item>
            <a-descriptions-item label="最后登录">{{ formatDateTime(detailUser.lastLoginAt) || '—' }}</a-descriptions-item>
          </a-descriptions>
        </a-tab-pane>

        <a-tab-pane key="skills" tab="提示词">
          <a-spin :spinning="skillsLoading">
            <div class="skill-section-title">我的提示词</div>
            <div class="skill-tab-toolbar">
              <a-space>
                <span>释放额度数量：</span>
                <a-input-number
                  v-model:value="releaseCount"
                  :min="1"
                  :max="100"
                  style="width: 120px"
                  placeholder="数量"
                />
                <a-button type="primary" :loading="releaseLoading" @click="handleReleaseCustomQuota">
                  释放额度
                </a-button>
              </a-space>
            </div>
            <a-table
              :columns="skillColumns"
              :data-source="userSkills"
              :pagination="false"
              :scroll="{ x: 'max-content' }"
              size="small"
              row-key="bizNo"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'auditStatus'">
                  <a-tag :color="auditStatusColor(record.auditStatus)">
                    {{ auditStatusLabel(record.auditStatus) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'prompt'">
                  <span :title="record.prompt">{{ record.prompt || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'scope'">
                  {{ record.scope || '—' }}
                </template>
                <template v-else-if="column.key === 'createdAt'">
                  {{ formatDateTime(record.createdAt) }}
                </template>
              </template>
            </a-table>
          </a-spin>

          <a-spin :spinning="publishedLoading">
            <div class="skill-section-title">发布的提示词</div>
            <div class="skill-tab-toolbar">
              <a-space>
                <span>释放发布额度：</span>
                <a-input-number
                  v-model:value="publishReleaseCount"
                  :min="1"
                  :max="100"
                  style="width: 120px"
                  placeholder="数量"
                />
                <a-button type="primary" :loading="publishReleaseLoading" @click="handleReleasePublishQuota">
                  释放额度
                </a-button>
              </a-space>
            </div>
            <a-table
              :columns="publishedSkillColumns"
              :data-source="publishedSkills"
              :pagination="false"
              :scroll="{ x: 'max-content' }"
              size="small"
              row-key="bizNo"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'auditStatus'">
                  <a-tag :color="publishStatusColor(record.auditStatus)">
                    {{ publishStatusLabel(record.auditStatus) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'prompt'">
                  <span :title="record.prompt">{{ record.prompt || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'promptSummary'">
                  <span :title="record.promptSummary">{{ record.promptSummary || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'scope'">
                  {{ record.scope || '—' }}
                </template>
                <template v-else-if="column.key === 'price'">
                  {{ record.price != null ? record.price.toFixed(2) : '—' }}
                </template>
                <template v-else-if="column.key === 'createdAt'">
                  {{ formatDateTime(record.createdAt) }}
                </template>
              </template>
            </a-table>
          </a-spin>

          <a-spin :spinning="favoriteLoading">
            <div class="skill-section-title">收藏的提示词</div>
            <a-table
              :columns="favoriteSkillColumns"
              :data-source="favoriteSkills"
              :pagination="false"
              :scroll="{ x: 'max-content' }"
              size="small"
              row-key="bizNo"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'prompt'">
                  <span :title="record.prompt">{{ record.prompt || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'promptSummary'">
                  <span :title="record.promptSummary">{{ record.promptSummary || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'scope'">
                  {{ record.scope || '—' }}
                </template>
                <template v-else-if="column.key === 'price'">
                  {{ record.price != null ? record.price.toFixed(2) : '—' }}
                </template>
                <template v-else-if="column.key === 'publisher'">
                  <span v-if="record.publisherEmail">
                    {{ record.publisherNickname || record.publisherEmail }}
                    <span style="color: #8c8c8c; font-size: 12px">({{ record.publisherEmail }})</span>
                  </span>
                  <span v-else style="color: #8c8c8c">—</span>
                </template>
                <template v-else-if="column.key === 'favoriteAt'">
                  {{ formatDateTime(record.favoriteAt) }}
                </template>
              </template>
            </a-table>
          </a-spin>

          <a-spin :spinning="learnedLoading">
            <div class="skill-section-title">学习的提示词</div>
            <a-table
              :columns="learnedColumns"
              :data-source="learnedMonths"
              :pagination="false"
              :scroll="{ x: 'max-content' }"
              size="small"
              row-key="period"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'skillCount'">
                  {{ record.skillCount || 0 }}
                </template>
                <template v-else-if="column.key === 'period'">
                  <span>
                    {{ record.period }}
                    <a-tag v-if="record.period === currentPeriod" color="blue" style="margin-left: 8px">当月</a-tag>
                  </span>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button
                    type="link"
                    size="small"
                    :loading="resettingPeriod === record.period"
                    @click="handleResetLearnedQuota(record)"
                  >
                    重置当月额度
                  </a-button>
                </template>
              </template>
            </a-table>
          </a-spin>

          <a-spin :spinning="learnedSkillsLoading">
            <div class="skill-section-title">学习产生的提示词</div>
            <a-table
              :columns="learnedSkillColumns"
              :data-source="learnedSkills"
              :pagination="false"
              :scroll="{ x: 'max-content' }"
              size="small"
              row-key="bizNo"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'auditStatus'">
                  <a-tag :color="auditStatusColor(record.auditStatus)">
                    {{ auditStatusLabel(record.auditStatus) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'prompt'">
                  <span :title="record.prompt">{{ record.prompt || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'scope'">
                  {{ record.scope || '—' }}
                </template>
                <template v-else-if="column.key === 'createdAt'">
                  {{ formatDateTime(record.createdAt) }}
                </template>
              </template>
            </a-table>
          </a-spin>
        </a-tab-pane>
        <a-tab-pane key="works" tab="作品">
          <a-spin :spinning="articlesLoading">
            <div class="article-tab-toolbar">
              <a-input
                v-model:value="articleKeyword"
                placeholder="标题或描述"
                allow-clear
                style="width: 240px"
                @press-enter="handleArticleSearch"
              />
              <a-button type="primary" @click="handleArticleSearch">查询</a-button>
              <a-button @click="resetArticleSearch">重置</a-button>
            </div>
            <a-table
              :columns="articleColumns"
              :data-source="articles"
              :pagination="false"
              :scroll="{ x: 'max-content' }"
              size="small"
              row-key="bizNo"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'title'">
                  <a-button type="link" size="small" @click="openArticleDetail(record)">
                    {{ record.title || '—' }}
                  </a-button>
                </template>
                <template v-else-if="column.key === 'description'">
                  <span :title="record.description">{{ record.description || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'platform'">
                  {{ record.platform || '—' }}
                </template>
                <template v-else-if="column.key === 'skill'">
                  {{ record.skill || '—' }}
                </template>
                <template v-else-if="column.key === 'wordCount'">
                  {{ record.wordCount || '—' }}
                </template>
                <template v-else-if="column.key === 'completedAt'">
                  {{ formatDateTime(record.completedAt) || '—' }}
                </template>
                <template v-else-if="column.key === 'createdAt'">
                  {{ formatDateTime(record.createdAt) }}
                </template>
              </template>
            </a-table>
            <div class="article-tab-pagination">
              <a-pagination
                :current="articlePage"
                :page-size="articlePageSize"
                :total="articleTotal"
                :page-size-options="['10', '20', '50']"
                show-size-changer
                show-total
                @change="handleArticlePageChange"
                @show-size-change="handleArticlePageChange"
              />
            </div>
          </a-spin>
        </a-tab-pane>
      </a-tabs>
    </a-modal>

    <!-- 邀请关系弹框 -->
    <a-modal
      v-model:open="inviteModalVisible"
      title="邀请关系"
      width="100%"
      :style="{ maxWidth: '1280px' }"
      :footer="null"
      @cancel="closeInviteModal"
    >
      <a-spin :spinning="inviteLoading">
        <div v-if="inviteDetail" class="invite-modal-content">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="当前用户">
              {{ inviteTarget?.nickname || inviteTarget?.email }}
              <span style="color: #8c8c8c">({{ inviteTarget?.email }})</span>
            </a-descriptions-item>
            <a-descriptions-item label="邀请码">{{ inviteDetail.inviteCode || '—' }}</a-descriptions-item>
            <a-descriptions-item label="邀请人" :span="2">
              <div v-if="inviteDetail.inviter">
                <div>
                  {{ inviteDetail.inviter.nickname || inviteDetail.inviter.email }}
                  <span style="color: #8c8c8c">({{ inviteDetail.inviter.email }})</span>
                </div>
                <div style="color: #8c8c8c; font-size: 12px; margin-top: 4px">
                  绑定时间：{{ formatDateTime(inviteDetail.inviter.createdAt) }}
                </div>
              </div>
              <span v-else style="color: #8c8c8c">—</span>
            </a-descriptions-item>
          </a-descriptions>

          <div class="invite-section-title">邀请列表（{{ inviteDetail.total || 0 }} 人）</div>
          <a-table
            :columns="inviteColumns"
            :data-source="inviteDetail.invitees"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'email'">
                {{ record.email }}
              </template>
              <template v-else-if="column.key === 'nickname'">
                {{ record.nickname || '—' }}
              </template>
              <template v-else-if="column.key === 'createdAt'">
                {{ formatDateTime(record.createdAt) || '—' }}
              </template>
            </template>
          </a-table>
          <div class="invite-tab-pagination">
            <a-pagination
              :current="invitePage"
              :page-size="invitePageSize"
              :total="inviteDetail.total || 0"
              :page-size-options="['10', '20', '50']"
              show-size-changer
              show-total
              @change="handleInvitePageChange"
              @show-size-change="handleInvitePageChange"
            />
          </div>
        </div>
      </a-spin>
    </a-modal>

    <!-- 手动创建用户弹框 -->
    <a-modal
      v-model:open="createModalVisible"
      title="手动创建用户"
      ok-text="创建"
      cancel-text="取消"
      :confirm-loading="createLoading"
      @ok="submitCreateForm"
      @cancel="closeCreateModal"
    >
      <a-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        layout="vertical"
      >
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="createForm.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="createForm.nickname" placeholder="请输入昵称" />
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password
            v-model:value="createForm.password"
            placeholder="留空则使用默认密码 Aichuangzuo@123"
          />
        </a-form-item>
        <a-form-item label="用户类型" name="userType">
          <a-radio-group v-model:value="createForm.userType">
            <a-radio :value="1">真实用户</a-radio>
            <a-radio :value="0">机器人</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 导入结果弹框 -->
    <a-modal
      v-model:open="importResultVisible"
      :title="importResult?.success ? '导入成功' : '导入失败'"
      :footer="null"
      @cancel="closeImportResultModal"
    >
      <div v-if="importResult" class="import-result">
        <p>
          共解析 <strong>{{ importResult.totalRows }}</strong> 行，成功导入
          <strong>{{ importResult.importedCount }}</strong> 条。
        </p>
        <div v-if="!importResult.success && importResult.errors?.length" class="import-errors">
          <div class="import-errors-title">错误明细：</div>
          <a-list
            :data-source="importResult.errors"
            size="small"
            :pagination="{ pageSize: 5 }"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <div class="import-error-item">
                  <div class="import-error-row">第 {{ item.rowIndex }} 行（{{ item.email || '邮箱为空' }}）</div>
                  <ul>
                    <li v-for="(err, idx) in item.errors" :key="idx">{{ err }}</li>
                  </ul>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </div>
      </div>
    </a-modal>
    <!-- 作品详情弹框 -->
    <a-modal
      v-model:open="articleDetailVisible"
      title="作品详情"
      width="960"
      :footer="null"
      @cancel="closeArticleDetailModal"
    >
      <a-spin :spinning="articleDetailLoading">
        <div v-if="articleDetail" class="article-detail-content">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="业务编号" :span="2">{{ articleDetail.bizNo }}</a-descriptions-item>
            <a-descriptions-item label="标题" :span="2">{{ articleDetail.title || '—' }}</a-descriptions-item>
            <a-descriptions-item label="平台">{{ articleDetail.platform || '—' }}</a-descriptions-item>
            <a-descriptions-item label="风格">{{ articleDetail.skill || '—' }}</a-descriptions-item>
            <a-descriptions-item label="模板">{{ articleDetail.template || '—' }}</a-descriptions-item>
            <a-descriptions-item label="字数">{{ articleDetail.wordCount || '—' }}</a-descriptions-item>
            <a-descriptions-item label="完成时间">{{ formatDateTime(articleDetail.completedAt) || '—' }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatDateTime(articleDetail.createdAt) }}</a-descriptions-item>
          </a-descriptions>
          <div class="article-detail-body">
            <div class="article-detail-body-title">正文</div>
            <div class="article-detail-body-text">{{ articleDetail.body || '—' }}</div>
          </div>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { CopyOutlined, DownOutlined, PlusOutlined, ReloadOutlined, UploadOutlined, DownloadOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { useUserManagement } from '@/composables/useUserManagement.js'
import { copyToClipboard } from '@/utils/clipboard.js'
import { getUser, getUserInvites, updateUser, listUserSkills, listUserPublishedSkills, listUserFavoriteSkills, listUserLearnedSkillsByMonth, resetLearnedSkillQuota, releaseCustomSkillQuota, releasePublishSkillQuota, importUsers, downloadUserImportTemplate } from '@/api/user.js'
import { listUserArticles, getArticleDetail } from '@/api/article.js'
import { fetchPlans } from '@/api/plan.js'

const {
  users,
  total,
  loading,
  page,
  pageSize,
  keyword,
  inviteCode,
  fetchUsers,
  handleSearch,
  handleReset,
  handlePageChange,
  handleStatusChange,
  handleResetPassword,
  handleCreateUser,
  handleDeleteUser,
  handleBatchDeleteUsers
} = useUserManagement()

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '邮箱/账号', dataIndex: 'email', key: 'email', width: 220 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '类型', dataIndex: 'userType', key: 'userType', width: 100 },
  { title: '邀请码', dataIndex: 'inviteCode', key: 'inviteCode', width: 140 },
  { title: '邀请人', key: 'inviter', width: 180 },
  { title: '邀请人数', key: 'invitedCount', width: 100 },
  { title: '会员套餐', dataIndex: 'membershipPlan', key: 'membershipPlan', width: 100 },
  { title: '会员到期', dataIndex: 'membershipExpireAt', key: 'membershipExpireAt', width: 170 },
  { title: '注册时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '最后登录', key: 'lastLoginAt', width: 170 },
  { title: '操作', key: 'actions', fixed: 'right', width: 100 }
]

const inviteColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '邮箱', dataIndex: 'email', key: 'email', ellipsis: true },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '绑定/注册时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 }
]

const resetPasswordVisible = ref(false)
const resetPasswordTarget = ref(null)
const detailVisible = ref(false)
const detailUser = ref(null)
const detailTabKey = ref('basic')
const userSkills = ref([])
const skillsLoading = ref(false)
const releaseCount = ref(1)
const releaseLoading = ref(false)
const publishReleaseCount = ref(1)
const publishReleaseLoading = ref(false)
const publishedSkills = ref([])
const publishedLoading = ref(false)
const favoriteSkills = ref([])
const favoriteLoading = ref(false)
const learnedMonths = ref([])
const learnedLoading = ref(false)
const learnedSkills = ref([])
const learnedSkillsLoading = ref(false)
const resettingPeriod = ref(null)

const articles = ref([])
const articlesLoading = ref(false)
const articleKeyword = ref('')
const articlePage = ref(1)
const articlePageSize = ref(10)
const articleTotal = ref(0)
const articleDetail = ref(null)
const articleDetailVisible = ref(false)
const articleDetailLoading = ref(false)

const plans = ref([])

const currentPeriod = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
})()

const skillColumns = [
  { title: '业务编号', dataIndex: 'bizNo', key: 'bizNo', width: 150 },
  { title: '提示词名称', dataIndex: 'skillName', key: 'skillName', width: 150 },
  { title: '提示词内容', dataIndex: 'prompt', key: 'prompt', ellipsis: true, width: 320 },
  { title: '适用范围', key: 'scope', width: 120 },
  { title: '使用次数', dataIndex: 'useCount', key: 'useCount', width: 90 },
  { title: '审核状态', key: 'auditStatus', width: 100 },
  { title: '创建时间', key: 'createdAt', width: 170 }
]

const publishedSkillColumns = [
  { title: '业务编号', dataIndex: 'bizNo', key: 'bizNo', width: 150 },
  { title: '提示词名称', dataIndex: 'skillName', key: 'skillName', width: 150 },
  { title: '提示词摘要', dataIndex: 'promptSummary', key: 'promptSummary', ellipsis: true, width: 260 },
  { title: '适用范围', key: 'scope', width: 120 },
  { title: '价格', key: 'price', width: 90 },
  { title: '累计使用', dataIndex: 'totalUses', key: 'totalUses', width: 90 },
  { title: '发布状态', key: 'auditStatus', width: 100 },
  { title: '创建时间', key: 'createdAt', width: 170 }
]

const learnedColumns = [
  { title: '月份', key: 'period', width: 120 },
  { title: '已用次数', dataIndex: 'usedCount', key: 'usedCount', width: 100 },
  { title: '预扣次数', dataIndex: 'preUsedCount', key: 'preUsedCount', width: 100 },
  { title: '产生提示词数', key: 'skillCount', width: 120 },
  { title: '操作', key: 'action', width: 140 }
]

const learnedSkillColumns = [
  { title: '业务编号', dataIndex: 'bizNo', key: 'bizNo', width: 150 },
  { title: '提示词名称', dataIndex: 'skillName', key: 'skillName', width: 150 },
  { title: '提示词内容', dataIndex: 'prompt', key: 'prompt', ellipsis: true, width: 320 },
  { title: '适用范围', key: 'scope', width: 120 },
  { title: '使用次数', dataIndex: 'useCount', key: 'useCount', width: 90 },
  { title: '审核状态', key: 'auditStatus', width: 100 },
  { title: '创建时间', key: 'createdAt', width: 170 }
]

const favoriteSkillColumns = [
  { title: '业务编号', dataIndex: 'bizNo', key: 'bizNo', width: 150 },
  { title: '提示词名称', dataIndex: 'skillName', key: 'skillName', width: 150 },
  { title: '提示词摘要', dataIndex: 'promptSummary', key: 'promptSummary', ellipsis: true, width: 240 },
  { title: '提示词内容', dataIndex: 'prompt', key: 'prompt', ellipsis: true, width: 260 },
  { title: '适用范围', key: 'scope', width: 120 },
  { title: '价格', key: 'price', width: 90 },
  { title: '发布者', key: 'publisher', width: 180 },
  { title: '收藏时间', key: 'favoriteAt', width: 170 }
]

const articleColumns = [
  { title: '标题', key: 'title', ellipsis: true, width: 200 },
  { title: '描述', key: 'description', ellipsis: true, width: 240 },
  { title: '平台', key: 'platform', width: 100 },
  { title: '风格', key: 'skill', width: 120 },
  { title: '字数', key: 'wordCount', width: 80 },
  { title: '完成时间', key: 'completedAt', width: 170 },
  { title: '创建时间', key: 'createdAt', width: 170 }
]

const inviteModalVisible = ref(false)
const inviteDetail = ref(null)
const inviteLoading = ref(false)
const inviteTarget = ref(null)
const invitePage = ref(1)
const invitePageSize = ref(10)

const selectedRowKeys = ref([])

const editModalVisible = ref(false)
const editFormRef = ref()
const editForm = reactive({
  id: null,
  email: '',
  nickname: '',
  status: 'enabled',
  userType: 1,
  membershipPlan: null,
  expireDate: null
})
const editLoading = ref(false)

const editRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 64, message: '昵称长度 1-64 字符', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }]
}

const createModalVisible = ref(false)
const createFormRef = ref()
const createForm = reactive({
  email: '',
  nickname: '',
  password: '',
  userType: 1
})
const createLoading = ref(false)

const importing = ref(false)
const importResultVisible = ref(false)
const importResult = ref(null)

const createRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 64, message: '昵称长度 1-64 字符', trigger: 'blur' }
  ],
  password: [
    { min: 6, max: 32, message: '密码不符合要求，长度需在 6-32 字符之间', trigger: 'blur' }
  ],
  userType: [
    { required: true, message: '请选择用户类型', trigger: 'change' }
  ]
}

const planLabel = (code) => {
  const plan = plans.value.find((p) => p.planKey === code)
  return plan ? (plan.displayName || plan.planKey) : code
}

const formatDateTime = (s) => {
  if (!s) return ''
  return s.replace('T', ' ').slice(0, 19)
}

const copyEmail = async (email) => {
  try {
    await copyToClipboard(email)
    message.success('邮箱已复制')
  } catch (error) {
    message.error('复制失败')
  }
}

const copyInviteCode = async (inviteCode) => {
  try {
    await copyToClipboard(inviteCode)
    message.success('邀请码已复制')
  } catch (error) {
    message.error('复制失败')
  }
}

const confirmStatusChange = (user) => {
  const nextStatus = user.status === 'enabled' ? 'disabled' : 'enabled'
  Modal.confirm({
    title: `确定${nextStatus === 'enabled' ? '启用' : '禁用'}该用户？`,
    okText: '确认',
    cancelText: '取消',
    onOk: () => handleStatusChange(user)
  })
}

const confirmDelete = (user) => {
  Modal.confirm({
    title: '确定删除该用户？',
    content: `账号：${user.email}，删除后不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: () => handleDeleteUser(user)
  })
}

const onSelectChange = (keys) => {
  selectedRowKeys.value = keys
}

const confirmBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请至少选择一个用户')
    return
  }
  Modal.confirm({
    title: `确定删除选中的 ${selectedRowKeys.value.length} 个用户？`,
    content: '删除后不可恢复，请谨慎操作。',
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await handleBatchDeleteUsers(selectedRowKeys.value)
      selectedRowKeys.value = []
    }
  })
}

const openEditModal = async (user) => {
  editLoading.value = true
  try {
    const detail = await getUser(user.id)
    editForm.id = detail.id
    editForm.email = detail.email
    editForm.nickname = detail.nickname
    editForm.status = detail.status
    editForm.userType = detail.userType === 'robot' ? 0 : 1
    editForm.membershipPlan = detail.membershipPlan || null
    editForm.expireDate = detail.membershipExpireAt ? detail.membershipExpireAt.substring(0, 10) : null
    editModalVisible.value = true
  } finally {
    editLoading.value = false
  }
}

const closeEditModal = () => {
  editModalVisible.value = false
  editFormRef.value?.resetFields()
}

const submitEditForm = () => {
  editFormRef.value?.validate().then(async () => {
    editLoading.value = true
    try {
      await updateUser(editForm.id, {
        email: editForm.email.trim(),
        nickname: editForm.nickname.trim(),
        status: editForm.status,
        userType: editForm.userType,
        membershipPlan: editForm.membershipPlan || null,
        expireDate: editForm.expireDate || null
      })
      message.success('用户信息已更新')
      closeEditModal()
      fetchUsers()
    } finally {
      editLoading.value = false
    }
  })
}

const openCreateModal = () => {
  createForm.email = ''
  createForm.nickname = ''
  createForm.password = ''
  createForm.userType = 1
  createModalVisible.value = true
}

const closeCreateModal = () => {
  createModalVisible.value = false
  createFormRef.value?.resetFields()
}

const submitCreateForm = () => {
  createFormRef.value?.validate().then(async () => {
    createLoading.value = true
    try {
      await handleCreateUser({
        email: createForm.email.trim(),
        nickname: createForm.nickname.trim(),
        password: createForm.password,
        userType: createForm.userType
      })
      closeCreateModal()
    } catch (error) {
      // 错误已在 useUserManagement 中提示，此处仅阻止弹框关闭
    } finally {
      createLoading.value = false
    }
  })
}

const openResetPasswordModal = (user) => {
  resetPasswordTarget.value = user
  resetPasswordVisible.value = true
}

const confirmResetPassword = async () => {
  if (!resetPasswordTarget.value) return
  await handleResetPassword(resetPasswordTarget.value)
  resetPasswordVisible.value = false
}

const openDetailDrawer = async (user) => {
  detailTabKey.value = 'basic'
  releaseCount.value = 1
  publishReleaseCount.value = 1
  userSkills.value = []
  publishedSkills.value = []
  favoriteSkills.value = []
  learnedMonths.value = []
  learnedSkills.value = []
  articles.value = []
  articleKeyword.value = ''
  articlePage.value = 1
  articlePageSize.value = 10
  articleTotal.value = 0
  articleDetail.value = null
  try {
    detailUser.value = await getUser(user.id)
  } catch (error) {
    detailUser.value = user
  }
  detailVisible.value = true
  loadUserSkillsTab()
  loadUserPublishedSkillsTab()
  loadUserFavoriteSkillsTab()
  loadLearnedSkillsTab()
  loadLearnedSkillsListTab()
  loadUserArticles()
}

const closeDetailModal = () => {
  detailVisible.value = false
  detailUser.value = null
  detailTabKey.value = 'basic'
}

const loadUserSkillsTab = async () => {
  if (!detailUser.value?.id) return
  skillsLoading.value = true
  try {
    userSkills.value = await listUserSkills(detailUser.value.id, 1)
  } catch (error) {
    message.error(error.message || '加载我的提示词失败')
    userSkills.value = []
  } finally {
    skillsLoading.value = false
  }
}

const loadUserPublishedSkillsTab = async () => {
  if (!detailUser.value?.id) return
  publishedLoading.value = true
  try {
    publishedSkills.value = await listUserPublishedSkills(detailUser.value.id)
  } catch (error) {
    message.error(error.message || '加载发布的提示词失败')
    publishedSkills.value = []
  } finally {
    publishedLoading.value = false
  }
}

const loadUserFavoriteSkillsTab = async () => {
  if (!detailUser.value?.id) return
  favoriteLoading.value = true
  try {
    favoriteSkills.value = await listUserFavoriteSkills(detailUser.value.id)
  } catch (error) {
    message.error(error.message || '加载收藏的提示词失败')
    favoriteSkills.value = []
  } finally {
    favoriteLoading.value = false
  }
}

const loadLearnedSkillsTab = async () => {
  if (!detailUser.value?.id) return
  learnedLoading.value = true
  try {
    learnedMonths.value = await listUserLearnedSkillsByMonth(detailUser.value.id)
  } catch (error) {
    message.error(error.message || '加载学习的提示词失败')
    learnedMonths.value = []
  } finally {
    learnedLoading.value = false
  }
}

const loadLearnedSkillsListTab = async () => {
  if (!detailUser.value?.id) return
  learnedSkillsLoading.value = true
  try {
    learnedSkills.value = await listUserSkills(detailUser.value.id, 2)
  } catch (error) {
    message.error(error.message || '加载学习产生的提示词失败')
    learnedSkills.value = []
  } finally {
    learnedSkillsLoading.value = false
  }
}

const loadUserArticles = async () => {
  if (!detailUser.value?.id) return
  articlesLoading.value = true
  try {
    const res = await listUserArticles({
      userId: detailUser.value.id,
      keyword: articleKeyword.value,
      page: articlePage.value,
      pageSize: articlePageSize.value
    })
    articles.value = res.list || []
    articleTotal.value = res.total || 0
  } catch (error) {
    message.error(error.message || '加载作品列表失败')
    articles.value = []
    articleTotal.value = 0
  } finally {
    articlesLoading.value = false
  }
}

const handleArticleSearch = () => {
  articlePage.value = 1
  loadUserArticles()
}

const resetArticleSearch = () => {
  articleKeyword.value = ''
  articlePage.value = 1
  loadUserArticles()
}

const handleArticlePageChange = (p, size) => {
  articlePage.value = p
  if (size) articlePageSize.value = size
  loadUserArticles()
}

const openArticleDetail = async (record) => {
  articleDetailVisible.value = true
  articleDetailLoading.value = true
  try {
    articleDetail.value = await getArticleDetail(record.bizNo)
  } catch (error) {
    message.error(error.message || '加载作品详情失败')
    articleDetail.value = null
  } finally {
    articleDetailLoading.value = false
  }
}

const closeArticleDetailModal = () => {
  articleDetailVisible.value = false
  articleDetail.value = null
}

const handleReleaseCustomQuota = () => {
  if (!detailUser.value?.id) return
  if (!releaseCount.value || releaseCount.value < 1) {
    message.warning('请输入有效的释放数量')
    return
  }
  Modal.confirm({
    title: '确定释放自定义提示词额度？',
    content: `将为用户「${detailUser.value.nickname || detailUser.value.email}」释放 ${releaseCount.value} 个自定义提示词额度。`,
    okText: '确认释放',
    cancelText: '取消',
    onOk: async () => {
      releaseLoading.value = true
      try {
        await releaseCustomSkillQuota(detailUser.value.id, releaseCount.value)
        message.success('额度已释放')
        loadUserSkillsTab()
      } catch (error) {
        message.error(error.message || '释放额度失败')
      } finally {
        releaseLoading.value = false
      }
    }
  })
}

const handleReleasePublishQuota = () => {
  if (!detailUser.value?.id) return
  if (!publishReleaseCount.value || publishReleaseCount.value < 1) {
    message.warning('请输入有效的释放数量')
    return
  }
  Modal.confirm({
    title: '确定释放提示词市场发布额度？',
    content: `将为用户「${detailUser.value.nickname || detailUser.value.email}」释放 ${publishReleaseCount.value} 个提示词市场发布额度。`,
    okText: '确认释放',
    cancelText: '取消',
    onOk: async () => {
      publishReleaseLoading.value = true
      try {
        await releasePublishSkillQuota(detailUser.value.id, publishReleaseCount.value)
        message.success('发布额度已释放')
      } catch (error) {
        message.error(error.message || '释放发布额度失败')
      } finally {
        publishReleaseLoading.value = false
      }
    }
  })
}

const handleResetLearnedQuota = (record) => {
  if (!detailUser.value?.id) return
  Modal.confirm({
    title: `确定重置 ${record.period} 学习提示词额度？`,
    content: '重置后该月份的已用次数和预扣次数将清零，仅用于额度被异常占用的兜底场景。',
    okText: '确认重置',
    cancelText: '取消',
    onOk: async () => {
      resettingPeriod.value = record.period
      try {
        await resetLearnedSkillQuota(detailUser.value.id, record.period)
        message.success(`${record.period} 额度已重置`)
        loadLearnedSkillsTab()
      } catch (error) {
        message.error(error.message || '重置额度失败')
      } finally {
        resettingPeriod.value = null
      }
    }
  })
}

const auditStatusLabel = (status) => {
  const map = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
  return map[status] || '未知'
}

const auditStatusColor = (status) => {
  const map = { 0: 'default', 1: 'green', 2: 'red' }
  return map[status] || 'default'
}

const publishStatusLabel = (status) => {
  const map = { 0: '审核中', 1: '发布成功' }
  return map[status] || '未知'
}

const publishStatusColor = (status) => {
  const map = { 0: 'orange', 1: 'green' }
  return map[status] || 'default'
}

const openInviteModal = async (user, page = 1, pageSize = 10) => {
  inviteTarget.value = user
  invitePage.value = page
  invitePageSize.value = pageSize
  inviteLoading.value = true
  inviteModalVisible.value = true
  try {
    inviteDetail.value = await getUserInvites(user.id, invitePage.value, invitePageSize.value)
  } catch (error) {
    message.error(error.message || '加载邀请关系失败')
    inviteDetail.value = { userId: user.id, inviteCode: user.inviteCode, inviter: null, invitees: [], total: 0, page, pageSize }
  } finally {
    inviteLoading.value = false
  }
}

const closeInviteModal = () => {
  inviteModalVisible.value = false
  inviteDetail.value = null
  inviteTarget.value = null
  invitePage.value = 1
  invitePageSize.value = 10
}

const handleInvitePageChange = (p, size) => {
  if (!inviteTarget.value) return
  openInviteModal(inviteTarget.value, p, size || invitePageSize.value)
}

const downloadTemplate = async () => {
  try {
    const res = await downloadUserImportTemplate()
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '用户导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (error) {
    message.error(error.message || '下载模板失败')
  }
}

const beforeImport = (file) => {
  if (!file.name.toLowerCase().endsWith('.xlsx')) {
    message.error('请上传 .xlsx 格式的 Excel 文件')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    message.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

const handleImport = async ({ file }) => {
  importing.value = true
  try {
    const result = await importUsers(file)
    importResult.value = result
    importResultVisible.value = true
    if (result.success) {
      message.success(`成功导入 ${result.importedCount} 个用户`)
      fetchUsers()
    }
  } catch (error) {
    message.error(error.message || '导入失败')
  } finally {
    importing.value = false
  }
}

const closeImportResultModal = () => {
  importResultVisible.value = false
  importResult.value = null
}

const loadPlans = async () => {
  try {
    plans.value = await fetchPlans()
  } catch (error) {
    plans.value = []
  }
}

onMounted(() => {
  fetchUsers()
  loadPlans()
})
</script>

<style scoped>
.user-list-card {
  border-radius: 8px;
}

.user-list-header {
  margin-bottom: 16px;
}

.user-list-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.user-list-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.user-list-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.user-list-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.email-copy-btn {
  padding: 0 4px;
  height: auto;
  line-height: 1;
  vertical-align: middle;
  margin-left: 2px;
}

.email-copy-btn:hover {
  color: #07c160;
}

.invite-code-copy-btn {
  padding: 0 4px;
  height: auto;
  line-height: 1;
  vertical-align: middle;
  margin-left: 2px;
}

.invite-code-copy-btn:hover {
  color: #07c160;
}

.invite-modal-content .invite-section-title {
  margin: 16px 0 8px 0;
  font-weight: 500;
  font-size: 14px;
}

.invite-modal-content .ant-descriptions {
  margin-bottom: 8px;
}

.invite-tab-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.skill-tab-toolbar {
  margin-bottom: 16px;
}

.skill-section-title {
  margin: 24px 0 12px 0;
  font-weight: 600;
  font-size: 15px;
  color: #1a1a1a;
}

.skill-section-title:first-child {
  margin-top: 0;
}

.import-result p {
  margin: 0 0 12px 0;
}

.import-errors-title {
  font-weight: 500;
  margin-bottom: 8px;
}

.import-error-item {
  width: 100%;
}

.import-error-row {
  font-weight: 500;
  margin-bottom: 4px;
}

.import-error-item ul {
  margin: 0;
  padding-left: 18px;
}

.import-error-item li {
  color: #cf1322;
  font-size: 13px;
  line-height: 1.6;
}
.article-tab-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.article-tab-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.article-detail-content .article-detail-body {
  margin-top: 16px;
}

.article-detail-body-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 8px;
  color: #1a1a1a;
}

.article-detail-body-text {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #262626;
  background: #f6ffed;
  padding: 16px;
  border-radius: 8px;
  max-height: 420px;
  overflow-y: auto;
}
</style>
