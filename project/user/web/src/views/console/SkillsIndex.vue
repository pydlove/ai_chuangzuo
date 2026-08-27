<template>
  <div ref="stylesIndexRef" class="styles-index">
    <MobileConsoleHero
      title="我的提示词"
      desc="管理你的专属提示词，创作时一键选用。"
      logo-url="/assets/images/我的提示词logo-v1.png"
      image-url="/assets/images/我的提示词宣传图-v1.png"
    />

    <div class="styles-header">
      <div>
        <h2 class="styles-title">我的提示词</h2>
        <p class="styles-subtitle">管理你的专属提示词，创作时一键选用</p>
      </div>
    </div>

    <div class="styles-filter-bar">
      <div class="styles-tabs">
        <button
          :class="['styles-tab', { active: activeTab === 'my' }]"
          @click="activeTab = 'my'; editorMode = false"
        >
          我的提示词
        </button>
        <button
          :class="['styles-tab', { active: activeTab === 'learned' }]"
          @click="activeTab = 'learned'; editorMode = false"
        >
          学习的提示词
        </button>
        <button
          :class="['styles-tab', { active: activeTab === 'favorites' }]"
          @click="activeTab = 'favorites'; editorMode = false"
        >
          收藏的提示词
        </button>
        <button
          :class="['styles-tab', { active: activeTab === 'system' }]"
          @click="activeTab = 'system'; editorMode = false"
        >
          系统预设提示词
        </button>
      </div>

      <div class="styles-search">
        <input
          v-model="searchQuery"
          type="text"
          class="styles-search-input"
          placeholder="搜索提示词名或适用范围"
        />
      </div>
    </div>

    <!-- 我的提示词 -->
    <div v-show="activeTab === 'my'" class="styles-content">
      <div v-if="editorMode" class="style-editor">
        <div class="style-editor-header">
          <button class="style-editor-back" @click="goBack">← 返回</button>
          <div class="style-editor-title">{{ editingStyle.originalName ? '编辑提示词' : '新建我的提示词' }}</div>
        </div>
        <div class="style-editor-form">
          <div class="style-editor-field">
            <label class="style-editor-label">提示词名称 <span class="required">*</span></label>
            <input
              ref="nameInputRef"
              v-model="editingStyle.name"
              type="text"
              class="style-editor-input"
              placeholder="例如：我的小红书风"
              maxlength="20"
            />
            <div class="style-editor-counter" :class="{ over: (editingStyle.name || '').length > 20 }">
              {{ (editingStyle.name || '').length }} / 20
            </div>
            <div v-if="errors.name" class="style-editor-error">{{ errors.name }}</div>
          </div>
          <div class="style-editor-field">
            <label class="style-editor-label">简短描述</label>
            <input
              v-model="editingStyle.desc"
              type="text"
              class="style-editor-input"
              placeholder="一句话说明这个提示词适合写什么，例如：小红书种草笔记，语气亲切带 emoji"
              maxlength="100"
            />
            <div class="style-editor-counter" :class="{ over: (editingStyle.desc || '').length > 100 }">
              {{ (editingStyle.desc || '').length }} / 100
            </div>
            <div class="style-scope-hint">一句话让创作者快速了解你的提示词</div>
          </div>
          <div class="style-editor-field">
            <label class="style-editor-label">基于模版创建</label>
            <div class="template-switch-row">
              <a-switch
                v-model:checked="editingStyle.templateBased"
                checked-children="是"
                un-checked-children="否"
              />
              <span class="style-scope-hint">开启后按「角色 / 受众 / 写作要求 / 语气 / 禁区」五部分填写，保存时自动拼接为完整提示词</span>
            </div>
          </div>
          <template v-if="!editingStyle.templateBased">
            <div class="style-editor-field">
              <label class="style-editor-label style-editor-label--with-action">
                <span>提示词 <span class="required">*</span></span>
                <FullscreenOutlined class="style-editor-fullscreen-btn" title="全屏编辑" @click="openFullscreenPrompt('custom')" />
              </label>
              <textarea
                v-model="editingStyle.prompt"
                class="style-editor-textarea"
                placeholder="描述你希望 AI 采用的语气、结构、用词习惯等..."
                rows="5"
                :maxlength="promptMaxLength"
              ></textarea>
              <div class="style-editor-counter" :class="{ over: editingStyle.prompt.length > promptMaxLength }">
                {{ editingStyle.prompt.length }} / {{ promptMaxLength }}
              </div>
              <div v-if="errors.prompt" class="style-editor-error">{{ errors.prompt }}</div>
            </div>
          </template>
          <template v-else>
            <div class="style-editor-field">
              <label class="style-editor-label">角色 <span class="required">*</span></label>
              <textarea
                v-model="editingStyle.promptExtra.role"
                class="style-editor-textarea"
                placeholder="例如：你是一位擅长把专业知识翻译成大白话的科普作者"
                rows="2"
              ></textarea>
              <div v-if="errors.role" class="style-editor-error">{{ errors.role }}</div>
            </div>
            <div class="style-editor-field">
              <label class="style-editor-label">受众</label>
              <textarea
                v-model="editingStyle.promptExtra.audience"
                class="style-editor-textarea"
                placeholder="例如：对行业术语不熟悉但想快速理解的普通读者"
                rows="2"
              ></textarea>
            </div>
            <div class="style-editor-field">
              <label class="style-editor-label">写作要求 <span class="required">*</span></label>
              <textarea
                v-model="editingStyle.promptExtra.requirements"
                class="style-editor-textarea"
                placeholder="例如：1. 开篇从生活场景入手\n2. 必须使用至少一个比喻或类比\n3. 按「是什么→为什么→会怎样」推进"
                rows="5"
              ></textarea>
              <div v-if="errors.requirements" class="style-editor-error">{{ errors.requirements }}</div>
            </div>
            <div class="style-editor-field">
              <label class="style-editor-label">语气</label>
              <textarea
                v-model="editingStyle.promptExtra.tone"
                class="style-editor-textarea"
                placeholder="例如：耐心、亲切，像在给朋友讲一件有趣的事情"
                rows="2"
              ></textarea>
            </div>
            <div class="style-editor-field">
              <label class="style-editor-label">禁区</label>
              <textarea
                v-model="editingStyle.promptExtra.restrictions"
                class="style-editor-textarea"
                placeholder="例如：不要堆砌专业术语；不要给出无法验证的数据"
                rows="3"
              ></textarea>
            </div>
            <div class="style-editor-field">
              <label class="style-editor-label">拼接预览</label>
              <div class="template-prompt-preview">{{ displayPrompt }}</div>
              <div class="style-editor-counter" :class="{ over: displayPrompt.length > promptMaxLength }">
                {{ displayPrompt.length }} / {{ promptMaxLength }}
              </div>
              <div v-if="errors.prompt" class="style-editor-error">{{ errors.prompt }}</div>
            </div>
          </template>
          <div class="style-editor-field">
            <label class="style-editor-label">适用范围 <span class="required">*</span></label>
            <div class="style-scope-tags">
              <div
                v-for="tag in parseScopeTags(editingStyle.scope)"
                :key="tag"
                class="style-scope-tag"
              >
                {{ tag }}
                <span class="style-scope-tag-remove" @click="editingStyle.scope = removeScopeTag(editingStyle.scope, tag)">×</span>
              </div>
              <input
                v-if="parseScopeTags(editingStyle.scope).length < MAX_SCOPE_TAGS"
                v-model="editingStyleScopeInput"
                type="text"
                class="style-scope-tag-input"
                placeholder="输入标签后回车"
                :maxlength="MAX_SCOPE_TAG_LENGTH"
                @keydown.enter.prevent="addEditingStyleTag"
              />
            </div>
            <div class="style-scope-hint">最多 {{ MAX_SCOPE_TAGS }} 个标签，每个不超过 {{ MAX_SCOPE_TAG_LENGTH }} 个字</div>
            <div v-if="errors.scope" class="style-editor-error">{{ errors.scope }}</div>
          </div>
          <button
            class="save-style-btn"
            :disabled="!isFormValid"
            @click="saveStyle"
          >
            保存
          </button>
        </div>
      </div>

      <div v-else>
        <div v-if="canUseCustomStyles" class="styles-quota-hint">
          已创建 {{ customStyleQuotaText }} 个我的提示词
        </div>
        <div v-if="mySkills.length === 0" class="styles-empty">
          <div
            :class="['style-add-card', { locked: !canCreateCustom }]"
            @click="canCreateCustom && goToCreate()"
          >
            <div v-if="!canUseCustomStyles" class="style-add-badge required basic">需基础版</div>
            <div v-else-if="isCustomQuotaFull" class="style-add-badge quota">额度已满</div>
            <div class="style-add-icon">+</div>
            <div class="style-add-text">新建我的提示词</div>
          </div>
        </div>
        <div v-else class="styles-grid">
          <div
            :class="['style-add-card', { locked: !canCreateCustom }]"
            @click="canCreateCustom && goToCreate()"
          >
            <div v-if="!canUseCustomStyles" class="style-add-badge required basic">需基础版</div>
            <div v-else-if="isCustomQuotaFull" class="style-add-badge quota">额度已满</div>
            <div class="style-add-icon">+</div>
            <div class="style-add-text">新建我的提示词</div>
          </div>
          <SkillCard
            v-for="s in mySkills"
            :key="s.name"
            :name="s.name"
            :desc="s.desc && s.desc !== '自定义提示词' ? s.desc : ''"
            :prompt="promptSummary(s.prompt)"
            :show-avatar="false"
            :actions="[
              { label: '使用', type: 'primary', handler: () => useStyle(s) },
              { label: getMarketStatus(s.bizNo) === '已打回' ? '重新发布' : '发布', type: 'primary', visible: !getMarketStatus(s.bizNo) || getMarketStatus(s.bizNo) === '已打回', disabled: publishBlocked, title: publishQuotaHint, badge: publishBlocked && publishTotal <= 0 ? { text: '专业版', class: 'pro' } : null, handler: () => openPublishConfirm(s, 'my') },
              { label: '查看', handler: () => openMyStylePromptModal(s) },
              { label: '编辑', visible: !getMarketStatus(s.bizNo) || getMarketStatus(s.bizNo) === '已打回', handler: () => goToEdit(s) },
              { label: '下架', visible: getMarketStatus(s.bizNo) === '已上架', handler: () => confirmUnpublish(s, 'my') },
              { label: '删除', type: 'danger', visible: !getMarketStatus(s.bizNo) || getMarketStatus(s.bizNo) === '已打回', handler: () => deleteSkill(s.name) }
            ]"
          >
            <template #status>
              <div
                v-if="getMarketStatus(s.bizNo)"
                class="style-card-status"
                :class="statusClass(s.bizNo)"
              >
                {{ getMarketStatus(s.bizNo) }}
              </div>
            </template>
            <template #meta>
              <div class="skill-card__meta-row">
                <span class="skill-card__scope-inline">
                  <span
                    v-for="t in parseScopeTags(s.scope).slice(0, 2)"
                    :key="t"
                    class="skill-card__tag-compact"
                  ># {{ t }}</span>
                  <span
                    v-if="parseScopeTags(s.scope).length > 2"
                    class="skill-card__tag-more"
                  >+{{ parseScopeTags(s.scope).length - 2 }}</span>
                  <span class="skill-card__mine-compact">我的</span>
                </span>
              </div>
            </template>
            <template #extra>
              <div class="skill-card__extra-row">
                <span>已用 {{ s.count }} 次</span>
              </div>
            </template>
          </SkillCard>
        </div>
        <div v-if="mySkills.length > 0" class="styles-pagination">
          <a-pagination
            v-model:current="mySkillsPage"
            v-model:pageSize="mySkillsPageSize"
            :total="mySkillsTotal"
            :page-size-options="['12', '24', '48']"
            show-size-changer
            show-quick-jumper
            @change="onMySkillsPageChange"
            @showSizeChange="onMySkillsPageSizeChange"
          />
        </div>
      </div>
    </div>

    <!-- 系统预设 -->
    <div v-show="activeTab === 'system'" class="styles-content">
      <div v-if="!canUseCustomStyles" class="styles-upgrade-banner">
        当前套餐不支持系统预设提示词，开通会员后即可解锁
      </div>
      <EmptyState v-else-if="systemSkillsDisplay.length === 0" title="没有找到匹配的系统预设提示词" compact size="sm" />
      <div v-else class="styles-grid">
        <SkillCard
          v-for="s in systemSkillsDisplay"
          :key="s.name"
          :name="s.name"
          :desc="s.desc"
          :prompt="promptSummary(s.prompt)"
          :show-avatar="false"
          :actions="[
            { label: '查看完整提示词', handler: () => openMyStylePromptModal(s, 'system') }
          ]"
        >
          <template #meta>
            <div class="skill-card__meta-row">
              <span class="skill-card__scope-inline">
                <span
                  v-for="t in parseScopeTags(s.scope).slice(0, 2)"
                  :key="t"
                  class="skill-card__tag-compact"
                ># {{ t }}</span>
                <span
                  v-if="parseScopeTags(s.scope).length > 2"
                  class="skill-card__tag-more"
                >+{{ parseScopeTags(s.scope).length - 2 }}</span>
                <span class="skill-card__mine-compact">系统</span>
              </span>
            </div>
          </template>
        </SkillCard>
      </div>
      <div v-if="systemSkillsDisplay.length > 0" class="styles-pagination">
        <a-pagination
          v-model:current="systemSkillsPage"
          v-model:pageSize="systemSkillsPageSize"
          :total="systemSkillsTotal"
          :page-size-options="['12', '24', '48']"
          show-size-changer
          show-quick-jumper
          @change="onSystemSkillsPageChange"
          @showSizeChange="onSystemSkillsPageSizeChange"
        />
      </div>
    </div>

    <!-- 学习的提示词 -->
    <div v-show="activeTab === 'learned'" class="styles-content">
      <div v-if="canLearn" class="learned-banner">
        {{ learnBannerText }}
      </div>
      <div v-if="learnedSkillsDisplay.length === 0" class="styles-empty">
        <div v-if="isLearning" class="style-add-card learning-progress-card">>
          <div class="style-add-icon"><a-spin /></div>
          <div class="style-add-text learning-progress-text">灵犀同学正在帮您分析…</div>
        </div>
        <div v-else-if="learnedResult && !isEditingLearned" class="style-add-card pending-result-card" @click="resumeImportDialog">
          <div class="style-add-icon">✓</div>
          <div class="style-add-text">学习结果待保存，点击继续</div>
        </div>
        <div
          v-else-if="!isLearning && !learnedResult"
          :class="['style-add-card', { locked: !canLearn }]"
          @click="canLearn && handleOpenImportDialog()"
        >
          <div v-if="!canLearn && learnTotal <= 0" class="style-add-badge required pro">需专业版</div>
          <div v-else-if="!canLearn" class="style-add-badge quota">本月已满</div>
          <div class="style-add-icon">+</div>
          <div class="style-add-text">学习新提示词</div>
        </div>
      </div>
      <div v-else class="styles-grid">
        <div v-if="isLearning" class="style-add-card learning-progress-card">
          <div class="style-add-icon"><a-spin /></div>
          <div class="style-add-text learning-progress-text">灵犀同学正在帮您分析…</div>
        </div>
        <div v-else-if="learnedResult && !isEditingLearned" class="style-add-card pending-result-card" @click="resumeImportDialog">
          <div class="style-add-icon">✓</div>
          <div class="style-add-text">学习结果待保存，点击继续</div>
        </div>
        <div
          v-else-if="!isLearning && !learnedResult"
          :class="['style-add-card', { locked: !canLearn }]"
          @click="canLearn && handleOpenImportDialog()"
        >
          <div v-if="!canLearn && learnTotal <= 0" class="style-add-badge required pro">需专业版</div>
          <div v-else-if="!canLearn" class="style-add-badge quota">本月已满</div>
          <div class="style-add-icon">+</div>
          <div class="style-add-text">学习新提示词</div>
        </div>
        <SkillCard
          v-for="s in learnedSkillsDisplay"
          :key="s.name"
          :name="s.name"
          :desc="s.desc"
          :prompt="s.prompt"
          :show-avatar="false"
          avatar-variant="learned"
          :actions="[
            { label: '使用', type: 'primary', handler: () => useStyle(s) },
            { label: getMarketStatus(s.bizNo) === '已打回' ? '重新发布' : '发布', type: 'primary', visible: !getMarketStatus(s.bizNo) || getMarketStatus(s.bizNo) === '已打回', disabled: publishBlocked, title: publishQuotaHint, badge: publishBlocked && publishTotal <= 0 ? { text: '专业版', class: 'pro' } : null, handler: () => openPublishConfirm(s, 'learned') },
            { label: '查看', handler: () => openMyStylePromptModal(s, 'learned') },
            { label: '编辑', visible: !getMarketStatus(s.bizNo) || getMarketStatus(s.bizNo) === '已打回', handler: () => goToEditLearned(s) },
            { label: '下架', visible: getMarketStatus(s.bizNo) === '已上架', handler: () => confirmUnpublish(s, 'learned') },
            { label: '删除', type: 'danger', visible: !getMarketStatus(s.bizNo) || getMarketStatus(s.bizNo) === '已打回', handler: () => deleteLearnedStyle(s) }
          ]"
        >
          <template #status>
            <div
              v-if="getMarketStatus(s.bizNo)"
              class="style-card-status"
              :class="statusClass(s.bizNo)"
            >
              {{ getMarketStatus(s.bizNo) }}
            </div>
          </template>
          <template #meta>
            <div class="skill-card__meta-row">
              <span class="skill-card__scope-inline">
                <span
                  v-for="t in parseScopeTags(s.scope).slice(0, 2)"
                  :key="t"
                  class="skill-card__tag-compact"
                ># {{ t }}</span>
                <span
                  v-if="parseScopeTags(s.scope).length > 2"
                  class="skill-card__tag-more"
                >+{{ parseScopeTags(s.scope).length - 2 }}</span>
                <span class="skill-card__mine-compact">学习</span>
              </span>
            </div>
          </template>
          <template #extra>
            <div class="skill-card__extra-row">
              <span>学习 · {{ (s.createdAt || '').slice(0, 10) }}</span>
            </div>
          </template>
        </SkillCard>
      </div>
      <div v-if="learnedSkillsDisplay.length > 0" class="styles-pagination">
        <a-pagination
          v-model:current="learnedSkillsPage"
          v-model:pageSize="learnedSkillsPageSize"
          :total="learnedSkillsTotal"
          :page-size-options="['12', '24', '48']"
          show-size-changer
          show-quick-jumper
          @change="onLearnedSkillsPageChange"
          @showSizeChange="onLearnedSkillsPageSizeChange"
        />
      </div>
    </div>

    <!-- 收藏的提示词 -->
    <div v-show="activeTab === 'favorites'" class="styles-content">
      <div v-if="favoriteSkillsDisplay.length === 0" class="styles-empty">
        <EmptyState title="还没有收藏的提示词" action-text="去收藏" action-to="/console/skill-market" />
      </div>
      <div v-else class="styles-grid">
        <SkillCard
          v-for="s in favoriteSkillsDisplay"
          :key="s.id"
          :name="s.name"
          :desc="s.description || s.promptSummary || s.desc || ''"
          :prompt="s.prompt"
          :show-avatar="false"
          :class="{ 'favorite-offline': s.status !== 'approved' }"
          :actions="[
            { label: '使用', type: 'primary', disabled: s.status !== 'approved', title: s.status !== 'approved' ? '该提示词已下架' : '', handler: () => useFavoriteStyle(s) },
            { label: '查看', handler: () => openMyStylePromptModal(s, 'favorite') },
            { label: '取消收藏', type: 'danger', handler: () => confirmUnfavorite(s) }
          ]"
        >
          <template #status>
            <div
              v-if="s.status !== 'approved'"
              class="style-card-status offline"
            >
              已下架
            </div>
          </template>
          <template #meta>
            <div class="skill-card__meta-row">
              <span class="skill-card__creator">
                <span class="skill-card__creator-avatar">{{ (s.creatorName || '匿').charAt(0) }}</span>
                <span class="skill-card__creator-name">by {{ s.creatorName || '匿名用户' }}</span>
              </span>
              <span class="skill-card__scope-inline">
                <span
                  v-for="t in parseScopeTags(s.scope).slice(0, 2)"
                  :key="t"
                  class="skill-card__tag-compact"
                ># {{ t }}</span>
                <span
                  v-if="parseScopeTags(s.scope).length > 2"
                  class="skill-card__tag-more"
                >+{{ parseScopeTags(s.scope).length - 2 }}</span>
              </span>
            </div>
          </template>
        </SkillCard>
      </div>
      <div v-if="favoriteSkillsDisplay.length > 0" class="styles-pagination">
        <a-pagination
          v-model:current="favoriteSkillsPage"
          v-model:pageSize="favoriteSkillsPageSize"
          :total="favoriteSkillsTotal"
          :page-size-options="['12', '24', '48']"
          show-size-changer
          show-quick-jumper
          @change="onFavoriteSkillsPageChange"
          @showSizeChange="onFavoriteSkillsPageSizeChange"
        />
      </div>
    </div>
  </div>

  <!-- 学习提示词导入对话框 -->
  <a-modal
    :open="importDialogVisible"
    :footer="null"
    :width="640"
    centered
    class="learned-import-modal"
    @cancel="closeImportDialog"
    :maskClosable="!isLearning && !learnedResult"
    :keyboard="!isLearning && !learnedResult"
  >
    <template #title>
      <div class="modal-title">{{ isEditingLearned ? '编辑学习的提示词' : '学习写作提示词' }}</div>
    </template>

    <!-- 进度态 -->
    <div v-if="isLearning" class="learned-progress">
      <div class="learned-progress-bubble">
        <span class="learned-progress-text">
          <span
            v-for="(ch, i) in loadingChars"
            :key="i"
            class="learned-progress-char"
            :style="{ animationDelay: (i * 0.08) + 's' }"
          >{{ ch }}</span>
        </span>
        <span class="learned-progress-dots"><span></span><span></span><span></span></span>
      </div>
    </div>

    <!-- 粘贴正文 -->
    <template v-else-if="!learnedResult">
      <div class="learned-pane">
        <textarea
          v-model="pasteText"
          class="learned-textarea"
          placeholder="将原文粘贴到这里…"
          maxlength="1000"
        ></textarea>
        <div class="learned-counter">{{ pasteText.length }} / 1000</div>
        <div v-if="pasteError" class="learned-error">{{ pasteError }}</div>
        <button
          class="learned-submit-btn"
          :disabled="pasteText.trim().length < 200 || pasteText.trim().length > 1000"
          @click="submitPaste"
        >开始学习</button>
      </div>
    </template>

    <!-- 结果页 -->
    <div v-else>
      <div class="learned-result-title">{{ isEditingLearned ? '编辑提示词' : '学习结果 ✓ 已从参考文章中提取提示词' }}</div>
      <div id="learned-prompt-field" class="learned-result-field">
        <label class="learned-result-label">学到的提示词</label>
        <div class="learned-textarea-wrapper">
          <div class="learned-prompt-preview">{{ learnedResult.prompt }}</div>
          <button class="learned-prompt-edit-btn" @click="openFullscreenPrompt('learned')">
            <FullscreenOutlined />
            编辑
          </button>
        </div>
        <div class="learned-counter" :class="{ over: learnedResult.prompt.length > promptMaxLength }">
          {{ learnedResult.prompt.length }} / {{ promptMaxLength }}
        </div>
        <div v-if="learnedResult.prompt.length > promptMaxLength" class="learned-error">
          提示词超过 {{ promptMaxLength }} 字
        </div>
      </div>
      <div class="learned-result-field">
        <label class="learned-result-label">简短描述</label>
        <input
          v-model="learnedResult.desc"
          type="text"
          class="learned-input"
          placeholder="一句话说明这个提示词适合写什么，例如：小红书种草笔记，语气亲切带 emoji"
          maxlength="100"
        />
        <div class="style-editor-counter" :class="{ over: (learnedResult.desc || '').length > 100 }">
          {{ (learnedResult.desc || '').length }} / 100
        </div>
        <div class="style-scope-hint">一句话让创作者快速了解你的提示词</div>
      </div>
      <div class="learned-result-field">
        <label class="learned-result-label">原文提示词示例</label>
        <div class="learned-excerpt">① {{ learnedResult.excerpt1 }}</div>
        <div class="learned-excerpt">② {{ learnedResult.excerpt2 }}</div>
      </div>
      <div id="learned-scope-field" class="learned-result-field">
        <label class="learned-result-label">适用范围 <span class="required">*</span></label>
        <div class="style-scope-tags">
          <div
            v-for="tag in parseScopeTags(learnedResult.scope)"
            :key="tag"
            class="style-scope-tag"
          >
            {{ tag }}
            <span class="style-scope-tag-remove" @click="learnedResult.scope = removeScopeTag(learnedResult.scope, tag)">×</span>
          </div>
          <input
            v-if="parseScopeTags(learnedResult.scope).length < MAX_SCOPE_TAGS"
            v-model="learnedResultScopeInput"
            type="text"
            class="style-scope-tag-input"
            placeholder="输入标签后回车"
            :maxlength="MAX_SCOPE_TAG_LENGTH"
            @keydown.enter.prevent="addLearnedResultTag"
          />
        </div>
        <div class="style-scope-hint">最多 {{ MAX_SCOPE_TAGS }} 个标签，每个不超过 {{ MAX_SCOPE_TAG_LENGTH }} 个字</div>
        <div v-if="learnedScopeError" class="learned-error">{{ learnedScopeError }}</div>
        <div v-else-if="!learnedResult.scope || !parseScopeTags(learnedResult.scope).length" class="learned-hint">请至少添加一个适用范围标签</div>
      </div>
      <div id="learned-name-field" class="learned-result-field">
        <label class="learned-result-label">命名 <span class="required">*</span></label>
        <input
          v-model="learnedResult.name"
          type="text"
          class="learned-input"
          placeholder="例如：我的小红书风"
          maxlength="20"
        />
        <div class="style-editor-counter" :class="{ over: (learnedResult.name || '').length > 20 }">
          {{ (learnedResult.name || '').length }} / 20
        </div>
        <div v-if="learnedResult.name.trim().length > 20" class="learned-error">提示词名称最多 20 字</div>
        <div v-else-if="learnedResultError" class="learned-error">{{ learnedResultError }}</div>
      </div>
      <div class="learned-result-actions">
        <button class="learned-cancel-btn" :disabled="savingLearned" @click="closeImportDialog(true)">放弃</button>
       <button
         class="learned-submit-btn"
         :disabled="savingLearned"
         @click="saveLearnedResult"
       >{{ savingLearned ? '保存中...' : '保存到提示词' }}</button>
      </div>
    </div>
  </a-modal>

  <a-modal
    :open="publishConfirmVisible"
    title="发布提示词到市场"
    :footer="null"
    :width="480"
    centered
    @cancel="closePublishConfirm"
  >
    <div class="publish-confirm-body">
      <p class="publish-confirm-title">确认发布「{{ pendingPublish.style?.name }}」？</p>
      <div class="publish-confirm-quota">{{ publishQuotaHint }}</div>
      <ol class="publish-confirm-list">
        <li>发布后将进入<span class="publish-confirm-highlight">平台审核流程</span>，审核通过后即可在 提示词市场中发现并使用该提示词。</li>
        <li>审核期间该提示词会显示<span class="publish-confirm-highlight">「审核中」</span>状态，你可以随时查看进度。</li>
        <li>提示词被他人使用后，你将按照<span class="publish-confirm-highlight">收益规则</span>获得<span class="publish-confirm-highlight">创作币奖励</span>。</li>
      </ol>
      <p class="publish-confirm-tip">请确保提示词符合<span class="publish-confirm-highlight">平台规范</span>，避免违规内容。</p>
    </div>
    <div class="publish-confirm-actions">
      <button class="publish-confirm-cancel" @click="closePublishConfirm">取消</button>
      <button class="publish-confirm-submit" @click="confirmPublish">确认发布</button>
    </div>
  </a-modal>

  <!-- 提示词详情弹框 -->
  <SkillDetailModal
    :skill="selectedSkillForModal"
    :visible="myStylePromptVisible"
    :current-user-id="currentUserId"
    :is-favorite="selectedMyStyleSource === 'favorite'"
    :show-stats="false"
    @update:visible="closeMyStylePromptModal"
    @use="useSelectedStyle(selectedMyStyle, selectedMyStyleSource); closeMyStylePromptModal()"
    @toggle-favorite="confirmUnfavorite(selectedMyStyle); closeMyStylePromptModal()"
  >
    <template #footer-actions>
      <button
        v-if="selectedMyStyleSource !== 'system'"
        class="skill-detail-btn-use"
        :disabled="selectedMyStyleSource === 'favorite' && selectedMyStyle?.status !== 'approved'"
        :title="selectedMyStyleSource === 'favorite' && selectedMyStyle?.status !== 'approved' ? '该提示词已下架' : ''"
        @click="useSelectedStyle(selectedMyStyle, selectedMyStyleSource); closeMyStylePromptModal()"
      >使用</button>
      <button
        v-if="(selectedMyStyleSource === 'my' || selectedMyStyleSource === 'learned') && (!getMarketStatus(selectedMyStyle?.bizNo) || getMarketStatus(selectedMyStyle?.bizNo) === '已打回')"
        :class="['skill-detail-btn-fav', { active: false }]"
        :disabled="publishBlocked"
        :title="publishQuotaHint"
        @click="openPublishConfirm(selectedMyStyle, selectedMyStyleSource); closeMyStylePromptModal()"
      >{{ getMarketStatus(selectedMyStyle?.bizNo) === '已打回' ? '重新发布' : '发布' }}</button>
      <button
        v-if="(selectedMyStyleSource === 'my' || selectedMyStyleSource === 'learned') && (!getMarketStatus(selectedMyStyle?.bizNo) || getMarketStatus(selectedMyStyle?.bizNo) === '已打回')"
        class="skill-detail-btn-fav"
        @click="(selectedMyStyleSource === 'learned' ? goToEditLearned : goToEdit)(selectedMyStyle); closeMyStylePromptModal()"
      >编辑</button>
      <button
        v-if="(selectedMyStyleSource === 'my' || selectedMyStyleSource === 'learned') && getMarketStatus(selectedMyStyle?.bizNo) === '审核中'"
        class="skill-detail-btn-fav"
        disabled
      >审核中</button>
      <button
        v-if="(selectedMyStyleSource === 'my' || selectedMyStyleSource === 'learned') && getMarketStatus(selectedMyStyle?.bizNo) === '已上架'"
        class="skill-detail-btn-fav"
        @click="confirmUnpublish(selectedMyStyle, selectedMyStyleSource); closeMyStylePromptModal()"
      >下架</button>
      <button
        v-if="selectedMyStyleSource === 'favorite'"
        class="skill-detail-btn-fav"
        @click="confirmUnfavorite(selectedMyStyle); closeMyStylePromptModal()"
      >取消收藏</button>
    </template>
  </SkillDetailModal>

  <!-- 全屏编辑提示词 -->
  <a-modal
    v-model:open="fullscreenPromptVisible"
    title="全屏编辑提示词"
    :footer="null"
    :width="800"
    centered
    class="fullscreen-prompt-modal"
    @cancel="closeFullscreenPrompt"
  >
    <textarea
      v-model="fullscreenPromptText"
      class="fullscreen-prompt-textarea"
      :maxlength="promptMaxLength"
      rows="20"
      placeholder="描述你希望 AI 采用的语气、结构、用词习惯等..."
    ></textarea>
    <div class="fullscreen-prompt-footer">
      <span class="fullscreen-prompt-counter" :class="{ over: fullscreenPromptText.length > promptMaxLength }">
        {{ fullscreenPromptText.length }} / {{ promptMaxLength }}
      </span>
      <div class="fullscreen-prompt-actions">
        <button class="fullscreen-prompt-cancel" @click="closeFullscreenPrompt">取消</button>
        <button class="fullscreen-prompt-save" @click="saveFullscreenPrompt">确定</button>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import {
  systemSkills,
  mySkills,
  mySkillsTotal,
  currentSkill,
  applySkill,
  addCustomSkill,
  updateCustomSkill,
  removeCustomSkill,
  isSkillNameExists,
  removeLearnedSkill,
  analyzeArticleSkill,
  addLearnedSkill,
  isLearning,
  updateLearnedSkill,
  loadMySkills,
  loadLearnedSkills,
  loadSystemSkills
} from '@/composables/useSkills.js'
import {
  marketSkills,
  toggleFavorite,
  loadMarketSkills,
  loadFavoriteSkills,
  unpublishSkill,
  mySubmissions,
  loadMySubmissions,
  getMarketStatusByBizNo
} from '@/composables/useSkillMarket.js'
import { useBenefits } from '@/composables/useBenefits.js'
import { FullscreenOutlined } from '@ant-design/icons-vue'
import { publishSkill } from '@/api/skill.js'
import SkillCard from '@/components/SkillCard.vue'
import SkillDetailModal from '@/components/SkillDetailModal.vue'
import MobileConsoleHero from '@/components/MobileConsoleHero.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const SKILL_PROMPT_MAX_LENGTH = 1200

const router = useRouter()
const { benefitValue, benefitRemaining, loadBenefits } = useBenefits()
const stylesIndexRef = ref(null)
const currentUserId = localStorage.getItem('aichuangzuo_user_id') || ''
const activeTab = ref('my')
const mySkillsPage = ref(1)
const mySkillsPageSize = ref(12)

// 系统预设 / 学习 / 收藏 三 tab 的局部分页状态（与全局 ref 解耦，避免影响 main.js/CreateIndex 等）
const systemSkillsDisplay = ref([])
const systemSkillsPage = ref(1)
const systemSkillsPageSize = ref(12)
const systemSkillsTotal = ref(0)

const learnedSkillsDisplay = ref([])
const learnedSkillsPage = ref(1)
const learnedSkillsPageSize = ref(12)
const learnedSkillsTotal = ref(0)

const favoriteSkillsDisplay = ref([])
const favoriteSkillsPage = ref(1)
const favoriteSkillsPageSize = ref(12)
const favoriteSkillsTotal = ref(0)

const promptMaxLength = ref(SKILL_PROMPT_MAX_LENGTH)
const fullscreenPromptVisible = ref(false)
const fullscreenPromptText = ref('')
const fullscreenPromptTarget = ref('') // 'custom' | 'learned'
const nameInputRef = ref(null)

const openFullscreenPrompt = (target) => {
  fullscreenPromptTarget.value = target
  fullscreenPromptText.value = target === 'learned'
    ? (learnedResult.value?.prompt || '')
    : editingStyle.prompt
  fullscreenPromptVisible.value = true
}
const closeFullscreenPrompt = () => {
  fullscreenPromptVisible.value = false
}
const saveFullscreenPrompt = () => {
  if (fullscreenPromptTarget.value === 'learned' && learnedResult.value) {
    learnedResult.value.prompt = fullscreenPromptText.value
  } else {
    editingStyle.prompt = fullscreenPromptText.value
  }
  fullscreenPromptVisible.value = false
}

// 套餐权益相关
const styleCustomLimit = computed(() => parseInt(benefitValue('skill_custom') || '0', 10))
const canUseCustomStyles = computed(() => styleCustomLimit.value > 0)
const canCreateCustom = computed(() => canUseCustomStyles.value && mySkillsTotal.value < styleCustomLimit.value)
const isCustomQuotaFull = computed(() => canUseCustomStyles.value && mySkillsTotal.value >= styleCustomLimit.value)
const customStyleQuotaText = computed(() => `${mySkillsTotal.value} / ${styleCustomLimit.value}`)

const learnRemaining = computed(() => benefitRemaining('skill_learn_analyze'))
const learnTotal = computed(() => parseInt(benefitValue('skill_learn_analyze') || '0', 10))
const canLearn = computed(() => learnRemaining.value > 0)
const learnBannerText = computed(() => {
  if (isLearning.value) {
    return '● ● ● 灵犀同学正在帮您分析，请稍候…'
  }
  if (!canLearn.value) {
    if (learnTotal.value <= 0) return '当前套餐不支持 AI 提示词学习，升级专业版/旗舰版后解锁'
    return `本月学习额度已用完（${learnTotal.value} 次），下月 1 日重置`
  }
  return `本月还可学习 ${learnRemaining.value} 次 AI 提示词分析`
})

const publishRemaining = computed(() => benefitRemaining('skill_market_publish'))
const publishTotal = computed(() => parseInt(benefitValue('skill_market_publish') || '0', 10))

onMounted(async () => {
  await loadBenefits()
  await Promise.all([
    loadCurrentTab(),
    loadMarketSkills(),
    loadMySubmissions()
  ])
})

onUnmounted(() => {
  clearTimeout(searchDebounceTimer)
})

const MAX_SCOPE_TAGS = 3
const MAX_SCOPE_TAG_LENGTH = 8
const loadingText = '灵犀同学正在帮您分析...'
const loadingChars = loadingText.split('')

const parseScopeTags = (scopeStr) => {
  if (!scopeStr) return []
  return scopeStr.split(/[,，]/).map(t => t.trim()).filter(Boolean)
}

const formatScopeTags = (tags) => tags.join(',')

const validateScopeTags = (tags) => {
  if (tags.length === 0) return '请至少添加一个适用范围标签'
  if (tags.length > MAX_SCOPE_TAGS) return `最多添加 ${MAX_SCOPE_TAGS} 个标签`
  for (const tag of tags) {
    if (tag.length > MAX_SCOPE_TAG_LENGTH) return `每个标签最多 ${MAX_SCOPE_TAG_LENGTH} 个字`
  }
  return ''
}

const addScopeTag = (scopeStr, inputRef) => {
  const raw = inputRef.value.trim()
  if (!raw) return scopeStr
  const tags = parseScopeTags(scopeStr)
  if (tags.length >= MAX_SCOPE_TAGS) {
    inputRef.value = ''
    return scopeStr
  }
  const newTags = raw.split(/[,，]/).map(t => t.trim()).filter(Boolean)
  for (const tag of newTags) {
    if (tags.length >= MAX_SCOPE_TAGS) break
    if (tag.length > MAX_SCOPE_TAG_LENGTH) continue
    if (!tags.includes(tag)) tags.push(tag)
  }
  inputRef.value = ''
  return formatScopeTags(tags)
}

const removeScopeTag = (scopeStr, tag) => {
  return formatScopeTags(parseScopeTags(scopeStr).filter(t => t !== tag))
}

const addEditingStyleTag = () => {
  editingStyle.scope = addScopeTag(editingStyle.scope, editingStyleScopeInput)
}

const addLearnedResultTag = () => {
  if (!learnedResult.value) return
  learnedResult.value.scope = addScopeTag(learnedResult.value.scope, learnedResultScopeInput)
}

const searchQuery = ref('')
const searchKeyword = ref('')
let searchDebounceTimer = null

async function loadCurrentTab(keyword = '') {
  if (activeTab.value === 'my') {
    await loadMySkills(keyword, mySkillsPage.value, mySkillsPageSize.value)
  } else if (activeTab.value === 'learned') {
    const result = await loadLearnedSkills(keyword, learnedSkillsPage.value, learnedSkillsPageSize.value)
    learnedSkillsDisplay.value = result.list || []
    learnedSkillsTotal.value = result.total || 0
  } else if (activeTab.value === 'system') {
    const result = await loadSystemSkills(keyword, systemSkillsPage.value, systemSkillsPageSize.value)
    systemSkillsDisplay.value = result.list || []
    systemSkillsTotal.value = result.total || 0
  } else if (activeTab.value === 'favorites') {
    const result = await loadFavoriteSkills(keyword, favoriteSkillsPage.value, favoriteSkillsPageSize.value)
    favoriteSkillsDisplay.value = result.list || []
    favoriteSkillsTotal.value = result.total || 0
  }
}

const onMySkillsPageChange = (page) => {
  mySkillsPage.value = page
  loadCurrentTab(searchKeyword.value)
}

const onMySkillsPageSizeChange = (current, size) => {
  mySkillsPageSize.value = size
  mySkillsPage.value = 1
  loadCurrentTab(searchKeyword.value)
}

const onSystemSkillsPageChange = (page) => {
  systemSkillsPage.value = page
  loadCurrentTab(searchKeyword.value)
}

const onSystemSkillsPageSizeChange = (current, size) => {
  systemSkillsPageSize.value = size
  systemSkillsPage.value = 1
  loadCurrentTab(searchKeyword.value)
}

const onLearnedSkillsPageChange = (page) => {
  learnedSkillsPage.value = page
  loadCurrentTab(searchKeyword.value)
}

const onLearnedSkillsPageSizeChange = (current, size) => {
  learnedSkillsPageSize.value = size
  learnedSkillsPage.value = 1
  loadCurrentTab(searchKeyword.value)
}

const onFavoriteSkillsPageChange = (page) => {
  favoriteSkillsPage.value = page
  loadCurrentTab(searchKeyword.value)
}

const onFavoriteSkillsPageSizeChange = (current, size) => {
  favoriteSkillsPageSize.value = size
  favoriteSkillsPage.value = 1
  loadCurrentTab(searchKeyword.value)
}

watch(searchQuery, (val) => {
  clearTimeout(searchDebounceTimer)
  searchDebounceTimer = setTimeout(() => {
    searchKeyword.value = (val || '').trim()
  }, 300)
})

watch([activeTab, searchKeyword], () => {
  mySkillsPage.value = 1
  systemSkillsPage.value = 1
  learnedSkillsPage.value = 1
  favoriteSkillsPage.value = 1
  loadCurrentTab(searchKeyword.value)
}, { immediate: false })

// 导入对话框状态
const importDialogVisible = ref(false)
watch(importDialogVisible, (visible) => {
  const el = stylesIndexRef.value
  if (!el) return
  // 弹框打开时锁定本页及外层内容区滚动，避免背景抖动
  el.style.overflowY = visible ? 'hidden' : ''
  const consoleContent = el.closest('.console-content')
  if (consoleContent) {
    consoleContent.style.overflowY = visible ? 'hidden' : ''
  }
})
const pasteText = ref('')
const pasteError = ref('')
const learnedResult = ref(null)
const learnedResultScopeInput = ref('')
const learnedResultError = ref('')
const learnedScopeError = ref('')
const isEditingLearned = ref(false)
const editingLearnedOriginalName = ref('')
const savingLearned = ref(false)
const editorMode = ref(false)
const publishConfirmVisible = ref(false)
const pendingPublish = ref({ style: null, sourceType: '' })

watch(() => learnedResult.value?.scope, () => {
  if (learnedScopeError.value) learnedScopeError.value = ''
})
watch(() => learnedResult.value?.name, () => {
  if (learnedResultError.value) learnedResultError.value = ''
})
const myStylePromptVisible = ref(false)
const selectedMyStyle = ref(null)
const selectedMyStyleSource = ref('my')

const selectedSkillForModal = computed(() => {
  const s = selectedMyStyle.value
  if (!s) return null
  const source = selectedMyStyleSource.value
  const isMyOrLearned = source === 'my' || source === 'learned'
  return {
    ...s,
    creatorId: isMyOrLearned ? currentUserId : (s.creatorId || ''),
    creatorName: source === 'system' ? '系统' : source === 'favorite' ? (s.creatorName || '匿名用户') : (s.creatorName || '我'),
    prompt: s.prompt || s.promptSummary || '',
    desc: s.description || s.desc || '',
    scope: s.scope || '',
    createdAt: s.createdAt || null,
    approvedAt: s.approvedAt || null,
   excerpt1: s.excerpt1 || '',
   excerpt2: s.excerpt2 || ''
 }
})

const DEFAULT_PROMPT_EXTRA = {
  role: '',
  audience: '',
  requirements: '',
  tone: '',
  restrictions: ''
}

const editingStyle = reactive({
  originalName: '',
  name: '',
  desc: '',
  prompt: '',
  scope: '',
  templateBased: false,
  promptExtra: { ...DEFAULT_PROMPT_EXTRA }
})
const editingStyleScopeInput = ref('')

const errors = reactive({
  name: '',
  prompt: '',
  scope: '',
  role: '',
  requirements: ''
})

const promptSummary = (prompt) => {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

const displayPrompt = computed(() => {
  if (!editingStyle.templateBased) return editingStyle.prompt
  return buildPromptFromExtra(editingStyle.promptExtra)
})

function buildPromptFromExtra(extra) {
  const parts = []
  if (extra.role?.trim()) parts.push(`- 角色：${extra.role.trim()}`)
  if (extra.audience?.trim()) parts.push(`- 受众：${extra.audience.trim()}`)
  if (extra.requirements?.trim()) parts.push(`- 写作要求：\n${extra.requirements.trim()}`)
  if (extra.tone?.trim()) parts.push(`- 语气：${extra.tone.trim()}`)
  if (extra.restrictions?.trim()) parts.push(`- 禁区：${extra.restrictions.trim()}`)
  return parts.join('\n\n')
}

function parsePromptExtra(raw) {
  if (!raw) return null
  try {
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw
    return obj && obj.templateBased === true ? obj : null
  } catch (e) {
    return null
  }
}

watch(() => editingStyle.promptExtra, () => {
  if (editingStyle.templateBased) {
    editingStyle.prompt = buildPromptFromExtra(editingStyle.promptExtra)
  }
}, { deep: true })

watch(() => editingStyle.templateBased, (val) => {
  if (val) {
    editingStyle.prompt = buildPromptFromExtra(editingStyle.promptExtra)
  }
})

const validate = () => {
  errors.name = ''
  errors.prompt = ''
  errors.scope = ''
  errors.role = ''
  errors.requirements = ''

  const name = editingStyle.name.trim()
  const prompt = displayPrompt.value.trim()
  const scope = editingStyle.scope.trim()
  let valid = true

  if (!name) {
    errors.name = '请输入提示词名称'
    valid = false
  } else if (name.length > 20) {
    errors.name = '提示词名称最多 20 字'
    valid = false
  } else if (isSkillNameExists(name, editingStyle.originalName)) {
    errors.name = '该提示词名称已存在'
    valid = false
  }

  if (!prompt) {
    errors.prompt = '请输入提示词'
    valid = false
  } else if (prompt.length > promptMaxLength.value) {
    errors.prompt = `提示词最多 ${promptMaxLength.value} 字`
    valid = false
  }

  if (editingStyle.templateBased) {
    if (!editingStyle.promptExtra.role.trim()) {
      errors.role = '请填写角色'
      valid = false
    }
    if (!editingStyle.promptExtra.requirements.trim()) {
      errors.requirements = '请填写写作要求'
      valid = false
    }
  }

  const scopeTags = parseScopeTags(scope)
  const scopeError = validateScopeTags(scopeTags)
  if (scopeError) {
    errors.scope = scopeError
    valid = false
  }

  return valid
}

const isFormValid = computed(() => {
  const name = editingStyle.name.trim()
  const prompt = displayPrompt.value.trim()
  const scopeTags = parseScopeTags(editingStyle.scope)
  if (!name || name.length > 20) return false
  if (isSkillNameExists(name, editingStyle.originalName)) return false
  if (!prompt || prompt.length > promptMaxLength.value) return false
  if (editingStyle.templateBased) {
    if (!editingStyle.promptExtra.role.trim()) return false
    if (!editingStyle.promptExtra.requirements.trim()) return false
  }
  if (validateScopeTags(scopeTags)) return false
  return true
})

const goToCreate = () => {
  editingStyle.originalName = ''
  editingStyle.name = ''
  editingStyle.desc = ''
  editingStyle.prompt = ''
  editingStyle.scope = ''
  editingStyle.templateBased = false
  editingStyle.promptExtra = { ...DEFAULT_PROMPT_EXTRA }
  editingStyleScopeInput.value = ''
  errors.name = ''
  errors.prompt = ''
  errors.scope = ''
  errors.role = ''
  errors.requirements = ''
  editorMode.value = true
}

const goToEdit = (style) => {
  editingStyle.originalName = style.name
  editingStyle.name = style.name
  editingStyle.desc = style.desc === '自定义提示词' ? '' : (style.desc || '')
  editingStyle.prompt = style.prompt
  editingStyle.scope = style.scope || ''
  editingStyle.templateBased = false
  editingStyle.promptExtra = { ...DEFAULT_PROMPT_EXTRA }
  editingStyleScopeInput.value = ''
  errors.name = ''
  errors.prompt = ''
  errors.scope = ''
  errors.role = ''
  errors.requirements = ''
  const extra = parsePromptExtra(style.promptExtra)
  if (extra) {
    editingStyle.templateBased = true
    editingStyle.promptExtra = {
      role: extra.role || '',
      audience: extra.audience || '',
      requirements: extra.requirements || '',
      tone: extra.tone || '',
      restrictions: extra.restrictions || ''
    }
  }
  editorMode.value = true
}

const goBack = () => {
  editorMode.value = false
}

const saveStyle = async () => {
  if (!validate()) {
    if (errors.name === '该提示词名称已存在' && nameInputRef.value) {
      nameInputRef.value.focus()
    }
    return
  }
  try {
    const isCreate = !editingStyle.originalName
    const payload = {
      name: editingStyle.name,
      description: editingStyle.desc,
      prompt: displayPrompt.value,
      scope: editingStyle.scope,
      promptExtra: editingStyle.templateBased
        ? JSON.stringify({ templateBased: true, ...editingStyle.promptExtra })
        : null
    }
    if (isCreate) {
      await addCustomSkill(payload)
      mySkillsPage.value = 1
    } else {
      await updateCustomSkill(editingStyle.originalName, payload)
    }
    editorMode.value = false
    await loadCurrentTab(searchKeyword.value)
    if (!isCreate && currentSkill.value && currentSkill.value.name === editingStyle.originalName) {
      const updated = mySkills.value.find(s => s.name === editingStyle.name)
      if (updated) currentSkill.value = updated
    }
  } catch {
    // composable 已 message.error，弹框保持打开让用户修改
  }
}

const useStyle = (style) => {
  applySkill(style)
  router.push('/console/create')
}

const useFavoriteStyle = (style) => {
  if (style?.status !== 'approved') {
    message.warning('该提示词已下架，无法使用')
    return
  }
  router.push(`/console/create?marketSkillId=${style.id}`)
}

const deleteSkill = (name) => {
  Modal.confirm({
    title: '删除提示词',
    content: `确定要删除提示词「${name}」吗？删除后不可恢复。`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    centered: true,
    onOk: async () => {
      try {
        await removeCustomSkill(name)
        await loadCurrentTab(searchKeyword.value)
        if (mySkills.value.length === 0 && mySkillsPage.value > 1) {
          mySkillsPage.value -= 1
          await loadCurrentTab(searchKeyword.value)
        }
        if (currentSkill.value && currentSkill.value.name === name) {
          currentSkill.value = systemSkills.value[0] || null
        }
      } catch {
        // composable 已 message.error
      }
    }
  })
}

const handleOpenImportDialog = () => {
  if (!canLearn.value) {
    message.warning(learnBannerText.value)
    return
  }
  openImportDialog()
}

const openImportDialog = () => {
  if (!canLearn.value) {
    message.warning(learnBannerText.value)
    return
  }
  pasteText.value = ''
  pasteError.value = ''
  learnedResult.value = null
  learnedResultError.value = ''
  learnedScopeError.value = ''
  learnedResultScopeInput.value = ''
  isEditingLearned.value = false
  editingLearnedOriginalName.value = ''
  importDialogVisible.value = true
}

const goToEditLearned = (style) => {
  learnedResult.value = { ...style }
  learnedResultError.value = ''
  learnedScopeError.value = ''
  learnedResultScopeInput.value = ''
  isEditingLearned.value = true
  editingLearnedOriginalName.value = style.name
  importDialogVisible.value = true
}

const closeImportDialog = (clearResult = false) => {
  if (clearResult) {
    learnedResult.value = null
  }
  learnedResultError.value = ''
  learnedScopeError.value = ''
  importDialogVisible.value = false
  isEditingLearned.value = false
  editingLearnedOriginalName.value = ''
}

const resumeImportDialog = () => {
  if (!isLearning.value && !learnedResult.value) return
  importDialogVisible.value = true
}

const submitPaste = async () => {
  if (!canLearn.value) {
    message.warning(learnBannerText.value)
    return
  }
  pasteError.value = ''
  const text = pasteText.value.trim()
  if (text.length < 200) {
    pasteError.value = '正文过短（少于 200 字）'
    return
  }
  if (text.length > 1000) {
    pasteError.value = '正文过长（超过 1000 字）'
    return
  }
  await runAnalysis(text, 'paste')
}

const runAnalysis = async (text, sourceType) => {
  if (!canLearn.value) {
    message.warning(learnBannerText.value)
    return
  }
  if (isLearning.value) return
  isLearning.value = true
  try {
    const tempResult = await analyzeArticleSkill(text, { sourceType })
    learnedResult.value = { ...tempResult, name: '' }
  } catch (err) {
    message.error(err?.message || '分析失败，请重试')
  } finally {
    isLearning.value = false
  }
}

const canSaveLearnedResult = computed(() => {
  if (!learnedResult.value) return false
  const name = learnedResult.value.name.trim()
  if (!name || name.length > 20) return false
  if (learnedResult.value.prompt.length > promptMaxLength.value) return false
  const scopeTags = parseScopeTags(learnedResult.value.scope)
  if (scopeTags.length === 0 || validateScopeTags(scopeTags)) return false
  return true
})

const saveLearnedResult = async () => {
  if (!learnedResult.value || savingLearned.value) return
  const name = learnedResult.value.name.trim()
  const excludeName = isEditingLearned.value ? editingLearnedOriginalName.value : null

  const fail = async (msg, fieldId, errorType = 'name') => {
    if (errorType === 'scope') {
      learnedScopeError.value = msg
    } else if (errorType === 'name') {
      learnedResultError.value = msg
    }
    message.error(msg)
    if (fieldId) {
      await nextTick()
      const el = document.getElementById(fieldId)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' })
        el.classList.add('learned-field-shake')
        setTimeout(() => el.classList.remove('learned-field-shake'), 500)
      }
    }
  }

  if (!name) {
    await fail('请填写提示词名称', 'learned-name-field')
    return
  }
  if (isSkillNameExists(name, excludeName)) {
    await fail('该提示词名称已存在', 'learned-name-field')
    return
  }
  if (name.length > 20) {
    await fail('提示词名称最多 20 字', 'learned-name-field')
    return
  }
  if (learnedResult.value.prompt.length > promptMaxLength.value) {
    await fail(`提示词超过 ${promptMaxLength.value} 字`, 'learned-prompt-field', 'prompt')
    return
  }
  if (!learnedResult.value.scope || !parseScopeTags(learnedResult.value.scope).length) {
    await fail('请填写适用范围', 'learned-scope-field', 'scope')
    return
  }
  const scopeError = validateScopeTags(parseScopeTags(learnedResult.value.scope))
  if (scopeError) {
    await fail(scopeError, 'learned-scope-field', 'scope')
    return
  }
  savingLearned.value = true
  try {
    if (isEditingLearned.value) {
      await updateLearnedSkill(learnedResult.value.bizNo, learnedResult.value)
    } else {
      await addLearnedSkill(learnedResult.value)
    }
    await loadBenefits()
    learnedResult.value = null
    closeImportDialog()
    await loadCurrentTab(searchKeyword.value)
  } catch {
    // 错误提示已在 composable 内 message.error
  } finally {
    savingLearned.value = false
  }
}

const deleteLearnedStyle = (s) => {
  Modal.confirm({
    title: '删除提示词',
    content: `确定要删除「${s.name}」吗？删除后不可恢复，且不会恢复本月学习额度。`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    centered: true,
    onOk: async () => {
      try {
        await removeLearnedSkill(s.bizNo)
        await loadCurrentTab(searchKeyword.value)
      } catch {
        // composable 已 message.error
      }
    }
  })
}

const confirmUnfavorite = (s) => {
  Modal.confirm({
    title: '取消收藏',
    content: `确定要取消收藏「${s.name}」吗？`,
    okText: '取消收藏',
    cancelText: '再想想',
    okButtonProps: { danger: true },
    centered: true,
    onOk: async () => {
      try {
        await toggleFavorite(s.id)
        await loadCurrentTab(searchKeyword.value)
      } catch {
        // composable 已 message.error
      }
    }
  })
}

const getMarketStatus = (bizNo) => {
  return getMarketStatusByBizNo(bizNo)
}

const statusClass = (bizNo) => {
  const status = getMarketStatus(bizNo)
  if (status === '已上架') return 'approved'
  if (status === '审核中') return 'pending'
  if (status === '已打回') return 'rejected'
  return ''
}

/** 当前档位的发布额度（用于按钮 tooltip / 顶部 banner）。 */
const publishBlocked = computed(() => publishRemaining.value <= 0)
const publishQuotaHint = computed(() => {
  if (publishTotal.value <= 0) {
    return '当前套餐不支持发布到 提示词市场，请升级专业版/旗舰版会员'
  }
  if (publishRemaining.value <= 0) {
    return `发布额度已用完（${publishTotal.value} 个），升级套餐可发布更多提示词`
  }
  return `还可发布 ${publishRemaining.value} / ${publishTotal.value} 个提示词到市场`
})

const openPublishConfirm = (style, sourceType) => {
  pendingPublish.value = { style, sourceType }
  publishConfirmVisible.value = true
}

const confirmPublish = async () => {
  const { style, sourceType } = pendingPublish.value
  if (!style || !sourceType) return

  try {
    await publishSkill(style.bizNo)
    await loadMySubmissions()
    await loadCurrentTab(searchKeyword.value)
    message.success('提示词已提交审核')
  } catch (err) {
    message.error(err?.message || '提交失败')
  } finally {
    publishConfirmVisible.value = false
    pendingPublish.value = { style: null, sourceType: '' }
  }
}

const closePublishConfirm = () => {
  publishConfirmVisible.value = false
  pendingPublish.value = { style: null, sourceType: '' }
}

const confirmUnpublish = (style, sourceType) => {
  Modal.confirm({
    title: '下架提示词',
    content: `确定要下架已发布的提示词「${style.name}」吗？\n\n下架后：\n1. 其他人将无法在市场中看到该提示词；\n2. 本月发布额度将恢复 1 个；\n3. 已产生的收益不受影响。`,
    okText: '下架',
    cancelText: '取消',
    okButtonProps: { danger: true },
    centered: true,
    onOk: async () => {
      try {
        await unpublishSkill(style.bizNo)
        await Promise.all([
          loadBenefits(),
          loadMySubmissions(),
          loadMarketSkills(),
          loadCurrentTab(searchKeyword.value)
        ])
        message.success('提示词已下架，发布额度已恢复')
      } catch (err) {
        message.error(err?.message || '下架失败，请重试')
      }
    }
  })
}

const openMyStylePromptModal = (style, source = 'my') => {
  selectedMyStyle.value = style
  selectedMyStyleSource.value = source
  myStylePromptVisible.value = true
}

const closeMyStylePromptModal = () => {
  myStylePromptVisible.value = false
  selectedMyStyle.value = null
  selectedMyStyleSource.value = 'my'
}

const useSelectedStyle = (style, source) => {
  if (source === 'favorite' && style?.status !== 'approved') {
    message.warning('该提示词已下架，无法使用')
    return
  }
  if (source === 'favorite') {
    useFavoriteStyle(style)
  } else {
    useStyle(style)
  }
}
</script>

<style scoped>
.styles-index {
  width: 100%;
  height: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 32px;
  overflow-y: auto;
  box-sizing: border-box;
}

.styles-header {
  margin-bottom: 20px;
}

.styles-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.styles-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.styles-filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.styles-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
  height: 44px;
  width: fit-content;
}

.styles-tab {
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.styles-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.styles-search {
  display: flex;
  align-items: center;
}

.styles-search-input {
  width: 100%;
  min-width: 280px;
  max-width: 480px;
  height: 44px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}

.styles-search-input:focus {
  outline: none;
  border-color: #ff2442;
}

.styles-empty {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-lg);
}

.styles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-lg);
}

.style-add-card {
  border: 2px dashed #e8e8e8;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.25s ease;
  min-height: 220px;
  box-sizing: border-box;
  background: #fff;
}

.style-add-card:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.06);
}

.style-add-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #fff0f2;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
}

.style-add-text {
  font-size: 14px;
  color: #595959;
  font-weight: 500;
}

.style-add-card {
  position: relative;
}

.style-add-card.locked {
  cursor: not-allowed;
  background: #f5f5f5;
  border-color: #e8e8e8;
}

.style-add-card.locked:hover {
  border-color: #e8e8e8;
  background: #f5f5f5;
  transform: none;
  box-shadow: none;
}

.style-add-card.locked .style-add-icon {
  background: #f0f0f0;
  color: #8c8c8c;
}

.style-add-card.locked .style-add-text {
  color: #8c8c8c;
}

.style-add-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  pointer-events: none;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
}

.style-add-badge.required.basic {
  background: linear-gradient(135deg, #fffbe6, #fff3a3);
  color: #8c6b00;
}

.style-add-badge.required.pro {
  background: linear-gradient(135deg, #fff1b8, #ffd666);
  color: #874d00;
}

.style-add-badge.quota {
  background: #fff7e6;
  color: #fa8c16;
}
.learning-progress-card {
  cursor: default;
}

.learning-progress-text {
  color: var(--color-primary);
}

.learning-progress-card .style-add-icon :deep(.ant-spin-dot-item) {
  background-color: var(--color-primary);
}

.learning-progress-card:hover {
  border-color: #e8e8e8;
  background: #fff;
  transform: none;
  box-shadow: none;
}

.pending-result-card {
  cursor: pointer;
}

.pending-result-card .style-add-icon {
  background: #f6ffed;
  color: #52c41a;
}

.style-card-status {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 500;
  padding: 2px 7px;
  border-radius: 6px;
  white-space: nowrap;
}

.style-card-status.approved {
  background: #f6ffed;
  color: #52c41a;
}

.style-card-status.pending {
  background: #fff7e6;
  color: #fa8c16;
}

.style-card-status.rejected {
  background: #fff1f0;
  color: #ff4d4f;
}

.style-card-status.offline {
  background: #f5f5f5;
  color: #8c8c8c;
}

.favorite-offline {
  opacity: 0.7;
}

.publish-confirm-body {
  padding: 8px 0 16px;
}

.publish-confirm-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.publish-confirm-list {
  margin: 0 0 14px;
  padding-left: 18px;
  font-size: 14px;
  color: #595959;
  line-height: 1.7;
}

.publish-confirm-list li {
  margin-bottom: 8px;
}

.publish-confirm-tip {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.publish-confirm-quota {
  display: inline-block;
  font-size: 12px;
  color: #ff2442;
  background: #fff0f2;
  border: 1px solid #ffd1d9;
  border-radius: 12px;
  padding: 4px 12px;
  margin-bottom: 12px;
}

.publish-confirm-highlight {
  color: #ff2442;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.publish-confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.publish-confirm-cancel {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
}

.publish-confirm-submit {
  padding: 8px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.publish-confirm-submit:hover {
  background: #e61e3a;
}

.style-editor {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 24px;
  max-width: 720px;
}

.style-editor-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.style-editor-back {
  background: none;
  border: none;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 8px 4px 0;
}

.style-editor-back:hover {
  color: var(--color-primary);
}

.style-editor-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.style-editor-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.style-editor-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.style-editor-label {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.style-editor-label .required {
  color: #ff4d4f;
  margin-left: 2px;
}

.style-editor-input,
.style-editor-textarea {
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.style-editor-input:focus,
.style-editor-textarea:focus {
  border-color: #07c160;
  box-shadow: 0 0 0 2px rgba(7, 193, 96, 0.1);
}

.style-editor-error {
  color: #ff4d4f;
  font-size: 12px;
}

.style-scope-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  background: #fff;
  min-height: 44px;
  box-sizing: border-box;
}

.style-scope-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #fff0f2;
  border: 1px solid #ffd1d9;
  border-radius: 16px;
  font-size: 13px;
  color: #ff2442;
}

.style-scope-tag-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  cursor: pointer;
  color: #ff8a9b;
  font-size: 14px;
  line-height: 1;
}

.style-scope-tag-remove:hover {
  color: #ff2442;
  background: #ffe0e5;
}

.style-scope-tag-input {
  flex: 1;
  min-width: 80px;
  border: none;
  outline: none;
  font-size: 14px;
  color: #1a1a1a;
  background: transparent;
  padding: 4px 2px;
}

.style-scope-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 6px;
}

.style-editor-counter {
  text-align: right;
  font-size: 12px;
  color: #8c8c8c;
}

.style-editor-counter.over {
  color: #ff4d4f;
}

.save-style-btn {
  padding: 10px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  width: fit-content;
}

.save-style-btn:hover {
  background: #e61e3a;
}

.save-style-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.learned-banner {
  padding: 12px 16px;
  background: #fff0f2;
  border: 1px solid #ffd1d9;
  border-radius: 8px;
  font-size: 13px;
  color: #ff2442;
  margin-bottom: 16px;
}

.styles-upgrade-banner {
  padding: 12px 16px;
  background: #f5f5f5;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 13px;
  color: #595959;
  margin-bottom: 16px;
}

.styles-quota-hint {
  padding: 8px 0;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.styles-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.styles-pagination :deep(.ant-pagination) {
  color: var(--color-text-secondary);
}

.styles-pagination :deep(.ant-pagination-item) {
  background: var(--color-bg-card);
  border-color: var(--color-border-default);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.styles-pagination :deep(.ant-pagination-item a) {
  color: var(--color-text-secondary);
}

.styles-pagination :deep(.ant-pagination-item:hover) {
  border-color: var(--color-primary);
}

.styles-pagination :deep(.ant-pagination-item:hover a) {
  color: var(--color-primary);
}

.styles-pagination :deep(.ant-pagination-item-active) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.styles-pagination :deep(.ant-pagination-item-active a) {
  color: #fff;
}

.styles-pagination :deep(.ant-pagination-prev .ant-pagination-item-link) {
  background: var(--color-bg-card);
  border-color: var(--color-border-default);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.styles-pagination :deep(.ant-pagination-next .ant-pagination-item-link) {
  background: var(--color-bg-card);
  border-color: var(--color-border-default);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.styles-pagination :deep(.ant-pagination-prev:hover .ant-pagination-item-link) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.styles-pagination :deep(.ant-pagination-next:hover .ant-pagination-item-link) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.styles-pagination :deep(.ant-pagination-disabled .ant-pagination-item-link) {
  color: var(--color-text-placeholder);
  border-color: var(--color-border-default);
  cursor: not-allowed;
}

.styles-pagination :deep(.ant-pagination-disabled:hover .ant-pagination-item-link) {
  color: var(--color-text-placeholder);
  border-color: var(--color-border-default);
}

.styles-pagination :deep(.ant-pagination-jump-prev .ant-pagination-item-container .ant-pagination-item-link-icon),
.styles-pagination :deep(.ant-pagination-jump-next .ant-pagination-item-container .ant-pagination-item-link-icon) {
  color: var(--color-primary);
}

body[data-theme="dark"] .styles-pagination {
  border-top-color: #303030;
}

body[data-theme="dark"] .styles-pagination :deep(.ant-pagination-item) {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .styles-pagination :deep(.ant-pagination-item a) {
  color: #a6a6a6;
}

body[data-theme="dark"] .styles-pagination :deep(.ant-pagination-item:hover) {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .styles-pagination :deep(.ant-pagination-item:hover a) {
  color: var(--color-primary);
}

body[data-theme="dark"] .styles-pagination :deep(.ant-pagination-item-active) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .styles-pagination :deep(.ant-pagination-item-active a) {
  color: #fff;
}

body[data-theme="dark"] .styles-pagination :deep(.ant-pagination-prev .ant-pagination-item-link) {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .styles-pagination :deep(.ant-pagination-next .ant-pagination-item-link) {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .styles-pagination :deep(.ant-pagination-prev:hover .ant-pagination-item-link) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .styles-pagination :deep(.ant-pagination-next:hover .ant-pagination-item-link) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .styles-pagination :deep(.ant-pagination-disabled .ant-pagination-item-link) {
  color: #595959;
  border-color: #303030;
}

body[data-theme="dark"] .styles-pagination :deep(.ant-pagination-disabled:hover .ant-pagination-item-link) {
  color: #595959;
  border-color: #303030;
}

.style-editor-label--with-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.style-editor-fullscreen-btn {
  cursor: pointer;
  color: #8c8c8c;
  font-size: 14px;
  transition: color 0.2s;
}

.style-editor-fullscreen-btn:hover {
  color: var(--color-primary);
}

.fullscreen-prompt-textarea {
  width: 100%;
  min-height: 420px;
  padding: 14px;
  border: 1px solid #d9d9d9;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.8;
  font-family: inherit;
  resize: vertical;
  box-sizing: border-box;
  outline: none;
}

.fullscreen-prompt-textarea:focus {
  border-color: var(--color-primary);
}

.fullscreen-prompt-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
}

.fullscreen-prompt-counter {
  font-size: 12px;
  color: #8c8c8c;
}

.fullscreen-prompt-counter.over {
  color: #ff4d4f;
}

.fullscreen-prompt-actions {
  display: flex;
  gap: 10px;
}

.fullscreen-prompt-cancel,
.fullscreen-prompt-save {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #595959;
}

.fullscreen-prompt-save {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.fullscreen-prompt-save:hover {
  background: var(--color-primary-hover);
}

/* 学习写作提示词弹框的 subtab / pane / result 样式统一在下方全局 <style> 维护，避免与局部样式冲突 */

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.learned-hint {
  font-size: 12px;
  color: #8c8c8c;
}

/* ============ 移动端 ============
   - 4 个 tab 总宽超 375px → 改为横向滚动，不换行截字
   - 搜索框去除 min-width 限制，跟随容器宽度
*/
@media (max-width: 768px) {
  .styles-index {
    padding: 0 12px 16px;
  }

  .styles-header {
    display: none;
  }

  .styles-empty,
  .styles-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .styles-tabs {
    width: 100%;
    overflow-x: auto;
    overflow-y: hidden;
    flex-wrap: nowrap;
    scrollbar-width: none;
    height: auto;
    padding: 4px;
  }

  .styles-tabs::-webkit-scrollbar {
    display: none;
  }

  .styles-tab {
    flex-shrink: 0;
    white-space: nowrap;
    padding: 8px 14px;
  }

  .styles-search-input {
    min-width: 0;
    max-width: none;
  }
}

/* 深色模式 */
body[data-theme="dark"] .styles-tabs {
  background: #1a1a1a;
}

body[data-theme="dark"] .styles-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .styles-tab.active {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .styles-search-input {
  background: #141414;
  border-color: #303030;
  color: #f0f0f0;
}

body[data-theme="dark"] .style-add-card {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .learned-cancel-btn {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .styles-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .styles-tab:hover {
  color: #f0f0f0;
}

body[data-theme="dark"] .styles-empty {
  color: #a6a6a6;
}

body[data-theme="dark"] .styles-empty,
body[data-theme="dark"] .styles-empty .style-add-card {
  background-color: #141414 !important;
}

body[data-theme="dark"] .style-add-card:hover {
  border-color: var(--color-primary);
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .style-add-icon {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}

body[data-theme="dark"] .style-add-card.locked {
  background: #2a2a2a;
  border-color: #303030;
}

body[data-theme="dark"] .style-add-card.locked:hover {
  background: #2a2a2a;
  border-color: #303030;
}

body[data-theme="dark"] .style-add-card.locked .style-add-icon {
  background: #1f1f1f;
  color: #666;
}

body[data-theme="dark"] .style-add-card.locked .style-add-text {
  color: #666;
}

body[data-theme="dark"] .style-add-badge.required.basic {
  background: linear-gradient(135deg, #2b2611, #ad8b00);
  color: #fffbe6;
}

body[data-theme="dark"] .style-add-badge.required.pro {
  background: linear-gradient(135deg, #44311c, #d48806);
  color: #fffbe6;
}

body[data-theme="dark"] .style-add-badge.quota {
  background: #2b1a0a;
  color: #ffa940;
}

body[data-theme="dark"] .style-add-text {
  color: #d9d9d9;
}

body[data-theme="dark"] .learning-progress-text {
  color: #ff6b81;
}

body[data-theme="dark"] .learning-progress-card .style-add-icon :deep(.ant-spin-dot-item) {
  background-color: #ff6b81;
}

body[data-theme="dark"] .learning-progress-card:hover {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .pending-result-card .style-add-icon {
  background: rgba(82, 196, 26, 0.15);
  color: #4ade80;
}

body[data-theme="dark"] .style-card-status.approved {
  background: rgba(7, 193, 96, 0.15);
  color: #4ade80;
}

body[data-theme="dark"] .style-card-status.pending {
  background: rgba(250, 140, 22, 0.15);
  color: #ffa940;
}

body[data-theme="dark"] .style-card-status.rejected {
  background: rgba(255, 77, 79, 0.15);
  color: #ff7875;
}

body[data-theme="dark"] .style-card-status.offline {
  background: rgba(255, 255, 255, 0.06);
  color: #8c8c8c;
}

body[data-theme="dark"] .favorite-offline {
  opacity: 0.6;
}

body[data-theme="dark"] .publish-confirm-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .publish-confirm-list li,
body[data-theme="dark"] .publish-confirm-tip {
  color: #a6a6a6;
}

body[data-theme="dark"] .publish-confirm-highlight {
  color: var(--color-primary);
}

body[data-theme="dark"] .publish-confirm-quota {
  background: rgba(255, 36, 66, 0.15);
  border-color: rgba(255, 36, 66, 0.4);
  color: #ff4d6a;
}

body[data-theme="dark"] .publish-confirm-cancel {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .publish-confirm-submit {
  background: var(--color-primary);
}

body[data-theme="dark"] .publish-confirm-submit:hover {
  background: var(--color-primary-hover);
}

body[data-theme="dark"] .style-editor {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .style-editor-back {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-editor-back:hover {
  color: var(--color-primary);
}

body[data-theme="dark"] .style-editor-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-editor-label {
  color: #d9d9d9;
}

body[data-theme="dark"] .style-editor-input,
body[data-theme="dark"] .style-editor-textarea {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .style-editor-input::placeholder,
body[data-theme="dark"] .style-editor-textarea::placeholder {
  color: #737373;
}

body[data-theme="dark"] .style-editor-input:focus,
body[data-theme="dark"] .style-editor-textarea:focus {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-editor-error {
  color: #ff7875;
}

body[data-theme="dark"] .style-scope-tag {
  background: #2a2a2a;
  border-color: #434343;
  color: #d9d9d9;
}

body[data-theme="dark"] .style-scope-tag-remove {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-scope-tag-remove:hover {
  background: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .style-scope-tag-input {
  background: transparent;
  color: #f0f0f0;
}

body[data-theme="dark"] .style-scope-hint {
  color: #737373;
}

body[data-theme="dark"] .style-editor-counter {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-editor-counter.over {
  color: #ff7875;
}

body[data-theme="dark"] .save-style-btn {
  background: var(--color-primary);
}

body[data-theme="dark"] .save-style-btn:hover {
  background: var(--color-primary-hover);
}

body[data-theme="dark"] .save-style-btn:disabled {
  background: #434343;
  color: #737373;
}

body[data-theme="dark"] .learned-banner {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .styles-upgrade-banner {
  background: #1a1a1a;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .learned-textarea,
body[data-theme="dark"] .learned-input {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .learned-textarea::placeholder,
body[data-theme="dark"] .learned-input::placeholder {
  color: #737373;
}

body[data-theme="dark"] .learned-textarea:focus,
body[data-theme="dark"] .learned-input:focus {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .learned-counter {
  color: #a6a6a6;
}

body[data-theme="dark"] .learned-counter.over {
  color: #ff7875;
}

body[data-theme="dark"] .learned-error {
  color: #ff7875;
}

body[data-theme="dark"] .learned-submit-btn {
  background: var(--color-primary);
}

body[data-theme="dark"] .learned-progress-bubble {
  background: #1f1f1f;
  border-color: #2e2e2e;
}

body[data-theme="dark"] .learned-progress-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .learned-result-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .learned-result-field {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .learned-result-label {
  color: #d9d9d9;
}

body[data-theme="dark"] .learned-excerpt {
  background: #2a2a2a;
  border-color: #434343;
  color: #d9d9d9;
}

/* 适用范围 tag 输入容器（提示词编辑器内 + 学习结果区） */
body[data-theme="dark"] .style-scope-tags {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .style-scope-tag {
  background: rgba(255, 36, 66, 0.15);
  border-color: rgba(255, 36, 66, 0.4);
  color: #ff4d6a;
}

body[data-theme="dark"] .style-scope-tag-remove {
  color: #ff8a9b;
}

body[data-theme="dark"] .style-scope-tag-remove:hover {
  background: rgba(255, 36, 66, 0.25);
  color: #ff2442;
}

body[data-theme="dark"] .style-scope-tag-input {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-scope-tag-input::placeholder {
  color: #737373;
}

body[data-theme="dark"] .style-scope-hint {
  color: #a6a6a6;
}

body[data-theme="dark"] .modal-title {
  color: #f0f0f0;
}

/* 基于模版创建 */
.template-switch-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.template-switch-row .style-scope-hint {
  margin-top: 0;
}
.template-prompt-preview {
  width: 100%;
  min-height: 160px;
  padding: 14px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  font-family: inherit;
  background: #fafafa;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  box-sizing: border-box;
}
body[data-theme="dark"] .template-prompt-preview {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}
</style>

<style>
/* 学习提示词导入对话框：teleport 到 body，需非 scoped 全局覆盖 */
.learned-import-modal .ant-modal-body {
  display: flex;
  flex-direction: column;
  height: 420px;
  max-height: 420px;
  overflow-y: auto;
}

.learned-import-modal .ant-modal-body > * {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

body[data-theme="dark"] .learned-import-modal .ant-modal-content,
body[data-theme="dark"] .learned-import-modal .ant-modal-header {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}

body[data-theme="dark"] .learned-import-modal .ant-modal-close-x {
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .learned-import-modal .ant-modal-close:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}

/* 全屏编辑提示词弹框暗色 */
body[data-theme="dark"] .fullscreen-prompt-modal .ant-modal-content,
body[data-theme="dark"] .fullscreen-prompt-modal .ant-modal-header {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}
body[data-theme="dark"] .fullscreen-prompt-modal .ant-modal-title {
  color: #f0f0f0 !important;
}
body[data-theme="dark"] .fullscreen-prompt-modal .ant-modal-close-x {
  color: #a6a6a6 !important;
}
body[data-theme="dark"] .fullscreen-prompt-modal .ant-modal-close:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}
body[data-theme="dark"] .fullscreen-prompt-textarea {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}
body[data-theme="dark"] .fullscreen-prompt-textarea::placeholder {
  color: #737373;
}
body[data-theme="dark"] .fullscreen-prompt-textarea:focus {
  border-color: var(--color-primary);
}
body[data-theme="dark"] .fullscreen-prompt-counter {
  color: #a6a6a6;
}
body[data-theme="dark"] .fullscreen-prompt-counter.over {
  color: #ff7875;
}
body[data-theme="dark"] .fullscreen-prompt-cancel {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}
body[data-theme="dark"] .fullscreen-prompt-save {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
body[data-theme="dark"] .fullscreen-prompt-save:hover {
  background: var(--color-primary-hover);
}

/* 学习写作提示词弹框 */

.learned-pane {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.learned-pane .learned-textarea {
  flex: 1;
  min-height: 0;
}

.learned-textarea {
  width: 100%;
  min-height: 240px;
  padding: 14px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  font-family: inherit;
  background: #fafafa;
  resize: vertical;
  transition: all 0.2s ease;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.02);
}

.learned-textarea:hover {
  border-color: #c0c0c0;
}

.learned-textarea:focus {
  outline: none;
  background: #fff;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.08), inset 0 1px 2px rgba(0, 0, 0, 0.02);
}

.learned-textarea-wrapper {
  position: relative;
}

.learned-prompt-preview {
  width: 100%;
  min-height: 180px;
  padding: 14px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  font-family: inherit;
  background: #fafafa;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.learned-prompt-edit-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.learned-prompt-edit-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fff0f2;
}

.learned-field-shake {
  animation: learned-field-shake 0.4s ease;
}

@keyframes learned-field-shake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-4px); }
  40%, 80% { transform: translateX(4px); }
}

body[data-theme="dark"] .learned-prompt-preview {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .learned-prompt-edit-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .learned-prompt-edit-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(255, 36, 66, 0.12);
}

.learned-input {
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  font-size: 14px;
  background: #fafafa;
  transition: all 0.2s ease;
}

.learned-input:hover {
  border-color: #c0c0c0;
}

.learned-input:focus {
  outline: none;
  background: #fff;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.08);
}

.learned-counter {
  text-align: right;
  font-size: 12px;
  color: #8c8c8c;
  padding: 0 4px;
}

.learned-counter.over {
  color: #ff4d4f;
  font-weight: 500;
}

.learned-error {
  color: #ff4d4f;
  font-size: 13px;
  margin-top: 4px;
}

.learned-submit-btn {
  padding: 8px 20px;
  background: #fff;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  align-self: flex-end;
}

.learned-submit-btn:hover {
  background: var(--color-primary-bg);
}

.learned-submit-btn:disabled {
  background: #f5f5f5;
  border-color: #d9d9d9;
  color: #bfbfbf;
  cursor: not-allowed;
}

.learned-cancel-btn {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.learned-cancel-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fff0f2;
}

.learned-progress {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
}

.learned-progress-bubble {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: 16px;
  padding: 14px 18px;
}

.learned-progress-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.learned-progress-char {
  display: inline-block;
  animation: learned-char-wave 1.4s infinite ease-in-out;
}

.learned-progress-dots {
  display: inline-flex;
  gap: 3px;
  align-items: center;
}

.learned-progress-dots span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: learned-dot-bounce 1.2s infinite ease-in-out;
}

.learned-progress-dots span:nth-child(2) { animation-delay: 0.15s; }
.learned-progress-dots span:nth-child(3) { animation-delay: 0.3s; }

@keyframes learned-char-wave {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-4px); }
}

@keyframes learned-dot-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-3px); opacity: 1; }
}

.learned-result-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.learned-result-title::before {
  content: '✓';
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #52c41a;
  color: #fff;
  font-size: 13px;
}

.learned-result-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
}

.learned-result-label {
  font-size: 13px;
  font-weight: 600;
  color: #262626;
}

.learned-result-label .required {
  color: #ff4d4f;
}

.learned-excerpt {
  padding: 12px 14px;
  background: #fff;
  border-left: 3px solid var(--color-primary);
  border-radius: 0 8px 8px 0;
  font-size: 13px;
  color: #595959;
  line-height: 1.7;
  margin-bottom: 8px;
}

.learned-result-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  position: sticky;
  bottom: 0;
  margin-top: auto;
  padding: 16px 0;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.learned-import-modal .ant-modal-body {
  max-height: 70vh;
  overflow-y: auto;
}

.learned-result-actions .learned-cancel-btn:disabled,
.learned-result-actions .learned-submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

body[data-theme="dark"] .learned-textarea,
body[data-theme="dark"] .learned-input {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .learned-textarea:hover,
body[data-theme="dark"] .learned-input:hover {
  border-color: #555;
}

body[data-theme="dark"] .learned-textarea:focus,
body[data-theme="dark"] .learned-input:focus {
  background: #2a2a2a;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .learned-counter {
  color: #a6a6a6;
}

body[data-theme="dark"] .learned-counter.over {
  color: #ff7875;
}

body[data-theme="dark"] .learned-error {
  color: #ff7875;
}

body[data-theme="dark"] .learned-submit-btn {
  background: transparent;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .learned-submit-btn:hover {
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .learned-cancel-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .learned-cancel-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .learned-progress-bubble {
  background: #1f1f1f;
  border-color: #2e2e2e;
}

body[data-theme="dark"] .learned-progress-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .learned-result-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .learned-result-title::before {
  background: #4ade80;
}

body[data-theme="dark"] .learned-result-field {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .learned-result-label {
  color: #d9d9d9;
}

body[data-theme="dark"] .learned-excerpt {
  background: #2a2a2a;
  color: #d9d9d9;
}

body[data-theme="dark"] .learned-result-actions {
  background: #1f1f1f;
  border-color: #303030;
}
</style>
