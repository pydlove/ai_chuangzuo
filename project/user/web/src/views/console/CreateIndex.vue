<template>
  <div class="create-index">
    <div class="create-layout">
      <!-- 左侧创作区 -->
      <div class="create-card">
        <!-- 顶部：标题 + 额度 -->
        <div class="create-card-header">
          <div>
            <h2 class="create-title">开始创作</h2>
            <p class="create-subtitle">输入你的想法，AI 帮你生成文章</p>
          </div>
          <div class="quota-pill">
            本月剩余 <strong>{{ quotaRemaining }}</strong> / {{ quotaTotal }} 次
          </div>
        </div>

        <!-- 核心输入区 -->
        <div class="hero-input">
          <input
            v-model="customTitle"
            type="text"
            class="hero-title-input"
            placeholder="例如：职场新人快速提升效率的 5 个方法"
          />

          <textarea
            v-model="customRequirement"
            class="hero-textarea"
            placeholder="描述你想写的内容，或点击下方的灵感胶囊快速开始。例如：重点写时间管理技巧，加入具体案例，语气轻松、有干货。"
          ></textarea>
        </div>

        <!-- 智能默认配置 -->
        <div class="smart-defaults">
          <button class="settings-chip" @click="openPlatformModal">
            <span>{{ currentPlatform.name }}</span>
            <span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="openWordCountModal">
            <span>{{ currentWordCount.count }} 字 · {{ currentWordCount.label }}</span>
            <span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="openStyleModal">
            <span>{{ currentStyle.name }}</span>
            <span class="chip-caret">▾</span>
          </button>
          <button class="settings-chip" @click="openTemplateModal">
            <span>{{ currentTemplate.name }}</span>
            <span class="chip-caret">▾</span>
          </button>
        </div>

        <!-- 主操作行：保存草稿 + 生成文章 -->
        <div class="hero-action-row">
          <div class="hero-action-left">
            <button class="action-link" @click="handleSaveDraft">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 4px; vertical-align: -2px;">
                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                <polyline points="17 21 17 13 7 13 7 21"/>
                <polyline points="7 3 7 8 15 8"/>
              </svg>
              保存草稿
            </button>
            <button class="action-link" @click="router.push('/console/works?tab=drafts')">
              <FolderOutlined style="margin-right: 4px;" />
              草稿箱
            </button>
          </div>
          <button class="hero-generate-btn" @click="handleGenerate">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
            </svg>
            生成文章
          </button>
        </div>

        <!-- 选题灵感胶囊 -->
        <div class="topic-capsules">
          <span class="topic-capsules-label">没灵感？点一个快速开始：</span>
          <div class="topic-capsules-grid">
            <a-tooltip
              v-for="topic in topics"
              :key="topic.id"
              :title="topic.title"
              placement="top"
            >
              <button
                :class="['topic-capsule', { used: topic.used }]"
                :disabled="topic.used"
                :title="topic.title"
                @click="topic.used ? null : applyTopic(topic)"
              >
                {{ topic.title }}
              </button>
            </a-tooltip>
          </div>
          <button class="refresh-capsule" @click="refreshTopics">换一批</button>
        </div>
      </div>

    <!-- 右侧生成队列 -->
    <div class="queue-panel">
      <div class="queue-panel-header">
        <h3 class="queue-panel-title">生成队列</h3>
        <button class="queue-more-btn" @click="router.push('/console/works')">查看更多 →</button>
      </div>
      <div v-if="miniQueueList.length === 0" class="queue-panel-empty">
        <InboxOutlined class="empty-icon" />
        <div class="empty-text">暂无生成任务</div>
        <div class="empty-hint">点击「生成文章」开始创作</div>
      </div>
      <div v-else class="queue-panel-list">
        <div
          v-for="item in miniQueueList.slice(0, 5)"
          :key="item.id"
          :class="['queue-panel-item', item.status]"
        >
          <div class="queue-item-top">
            <div class="queue-item-icon">
              <LoadingOutlined v-if="item.status === 'generating'" :spin="true" />
              <CheckCircleOutlined v-else-if="item.status === 'completed'" />
              <ClockCircleOutlined v-else-if="item.status === 'queued'" />
              <CloseCircleOutlined v-else />
            </div>
            <div class="queue-item-info">
              <a-tooltip :title="item.title" placement="top">
                <span class="queue-item-title">{{ item.title }}</span>
              </a-tooltip>
              <div class="queue-item-meta">
                <span class="queue-item-status-badge" :class="item.status">{{ miniStatusText(item.status) }}</span>
                <span v-if="item.status === 'generating'" class="queue-item-progress-text">{{ Math.min(100, Math.round(item.progress)) }}%</span>
              </div>
            </div>
          </div>

          <!-- 生成中进度条 -->
          <div v-if="item.status === 'generating'" class="queue-item-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: Math.min(100, Math.round(item.progress)) + '%' }"></div>
            </div>
          </div>

          <!-- 已完成操作 -->
          <div v-if="item.status === 'completed'" class="queue-item-footer">
            <button class="queue-export-btn" @click="openExportModal(item)">
              <span>去导出</span>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </button>
          </div>
        </div>
        <div v-if="miniQueueList.length > 5" class="queue-panel-more">
          还有 {{ miniQueueList.length - 5 }} 个任务，<button class="queue-panel-more-link" @click="router.push('/console/works')">去我的作品查看 →</button>
        </div>
      </div>
    </div>

    <!-- 预览导出弹框 -->
    <a-modal
      v-model:open="exportModalVisible"
      :footer="null"
      :width="800"
      centered
      class="export-modal"
    >
      <template #title>
        <span class="modal-title-text">预览/导出</span>
      </template>

      <div v-if="exportArticle" class="export-modal-content">
        <div class="export-article">
          <h1 class="export-title">{{ exportArticle.title }}</h1>
          <div class="export-meta">
            <span>{{ exportArticle.completedAt }}</span>
            <span>·</span>
            <span>约 {{ exportArticle.wordCount }} 字</span>
            <span class="export-style-badge">风格:专业严谨</span>
          </div>
          <div class="export-body" v-html="formattedExportBody"></div>
        </div>

        <!-- 发布描述 -->
        <div class="export-publish-meta">
          <div class="export-meta-title">发布描述</div>
          <textarea
            v-model="exportPublishDesc"
            class="export-desc-input"
            placeholder="输入发布描述..."
          ></textarea>
          <div class="export-meta-actions">
            <button class="export-meta-btn" @click="regenerateExportDesc">换一版</button>
            <button class="export-meta-btn primary" @click="copyExportDesc">复制描述</button>
          </div>

          <div class="export-meta-title" style="margin-top: 20px;">推荐标签</div>
          <div class="export-tags">
            <span v-for="tag in exportTags" :key="tag" class="export-tag">{{ tag }}</span>
          </div>
          <div class="export-meta-actions">
            <button class="export-meta-btn" @click="regenerateExportTags">换一批</button>
            <button class="export-meta-btn primary" @click="copyExportTags">复制全部标签</button>
          </div>
        </div>

        <!-- 浮动操作栏 -->
        <div class="export-floating-bar">
          <button class="export-float-btn" @click="editExportArticle">编辑正文</button>
          <button class="export-float-btn" @click="optimizeExportTitle">✧ AI 优化标题</button>
          <button class="export-float-btn primary" @click="handleExportWord">导出 Word</button>
          <button class="export-float-btn outline" @click="copyExportText">复制正文</button>
          <button class="export-float-btn danger" @click="generateExportCards">生成贴图</button>
        </div>
      </div>
    </a-modal>

    <!-- AI 标题优化弹框 -->
    <a-modal
      v-model:open="titleOptVisible"
      :footer="null"
      :width="560"
      centered
      class="title-opt-modal"
    >
      <template #title>
        <span class="modal-title-text">AI 标题优化</span>
      </template>

      <div v-if="exportArticle" class="title-opt-content">
        <div class="title-opt-original">
          <span>原标题</span>{{ exportArticle.title }}
        </div>
        <div class="title-opt-section-label">AI 推荐标题</div>
        <div class="title-opt-list">
          <div
            v-for="(title, index) in titleOptSuggestions"
            :key="`ai-${index}`"
            :class="['title-opt-item', { selected: selectedOptTitle === title }]"
            @click="selectOptTitle(title)"
          >
            <div class="title-opt-radio"></div>
            <div class="title-opt-item-text">{{ title }}</div>
          </div>
        </div>
        <div class="title-opt-footer">
          <button class="title-opt-btn-cancel" @click="closeTitleOpt">取消</button>
          <button class="title-opt-btn-confirm" :disabled="!selectedOptTitle" @click="confirmOptTitle">确认替换</button>
        </div>
      </div>
    </a-modal>

    <!-- 贴图生成弹框 -->
    <a-modal
      v-model:open="cardsModalVisible"
      :footer="null"
      :width="900"
      centered
      class="cards-modal"
    >
      <template #title>
        <span class="modal-title-text">生成贴图</span>
      </template>

      <div class="cards-modal-content">
        <div class="cards-modal-header-row">
          <div class="cards-modal-tabs">
            <button
              v-for="key in cardStyleKeys"
              :key="key"
              class="cards-modal-tab"
              :class="{ active: cardsStyle === key }"
              :style="cardsStyle === key ? { background: cardStyles[key].accent, borderColor: cardStyles[key].accent, color: '#fff' } : {}"
              @click="cardsStyle = key"
            >
              {{ cardStyles[key].label }}
            </button>
          </div>
          <button class="cards-modal-download-all" @click="downloadAllExportCards">全部下载</button>
        </div>

        <div class="cards-modal-sub">
          共 {{ cardsData.length }} 张 · {{ cardStyles[cardsStyle].label }}风格 · 点击单张可下载
        </div>

        <div class="cards-modal-grid">
          <div
            v-for="(card, index) in cardsData"
            :key="index"
            class="cards-modal-item"
            @click="downloadExportCard(index)"
          >
            <canvas
              :ref="el => { if (el) cardCanvasRefs[index] = el }"
              class="cards-modal-canvas"
              width="750"
              height="1000"
            ></canvas>
            <div class="cards-modal-item-label">
              图 {{ index + 1 }} / {{ cardsData.length }}{{ card.type === 'cover' ? ' · 封面' : ' · ' + card.title }}
            </div>
          </div>
        </div>
      </div>
    </a-modal>
  </div>

    <!-- 发布平台选择弹框 -->
    <a-modal
      v-model:open="platformVisible"
      :footer="null"
      :width="560"
      centered
      :closable="true"
    >
      <template #title>
        <div class="modal-title-wrap">
          <div class="modal-title">选择发布平台</div>
          <div class="modal-subtitle">选择目标平台，AI 将按平台规则推荐模板、字数和标签</div>
        </div>
      </template>
      <div class="platform-grid">
        <div
          v-for="p in platforms"
          :key="p.key"
          :class="['platform-item', { selected: currentPlatform.key === p.key }]"
          @click="selectPlatform(p)"
        >
          <div class="platform-name">{{ p.name }}</div>
          <div class="platform-desc">{{ p.desc }}</div>
        </div>
      </div>
    </a-modal>

    <!-- 字数选择弹框 -->
    <a-modal
      v-model:open="wordCountVisible"
      :footer="null"
      :width="640"
      centered
    >
      <template #title>
        <div class="modal-title-wrap">
          <div class="modal-title">设置文章字数</div>
          <div class="modal-subtitle">选择合适的字数，让 AI 写出更精准的内容</div>
        </div>
      </template>

      <div class="wc-tabs">
        <button
          v-for="tab in wordCountTabs"
          :key="tab.key"
          :class="['wc-tab', { active: wordCountTab === tab.key }]"
          @click="wordCountTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="wc-content">
        <!-- 按平台 -->
        <div v-if="wordCountTab === 'platform'" class="wc-grid">
          <div
            v-for="wc in platformWordCounts"
            :key="wc.count"
            :class="['wc-item', { selected: currentWordCount.count === wc.count }]"
            @click="selectWordCount(wc)"
          >
            <div class="wc-count">{{ wc.count }} 字</div>
            <div class="wc-label">{{ wc.label }}</div>
          </div>
        </div>

        <!-- 按场景 -->
        <div v-else-if="wordCountTab === 'scenario'" class="wc-list">
          <div
            v-for="s in wordCountPresets.scenario"
            :key="s.count"
            :class="['wc-item-wide', { selected: currentWordCount.count === s.count }]"
            @click="selectWordCount(s)"
          >
            <div class="wc-item-left">
              <div class="wc-count">{{ s.count }} 字</div>
              <div class="wc-label">{{ s.label }}</div>
            </div>
            <div class="wc-desc">{{ s.desc }}</div>
          </div>
        </div>

        <!-- 按档位 -->
        <div v-else-if="wordCountTab === 'tier'" class="wc-list">
          <div
            v-for="t in wordCountPresets.tier"
            :key="t.count"
            :class="['wc-item-wide', { selected: currentWordCount.count === t.count }]"
            @click="selectWordCount(t)"
          >
            <div class="wc-item-left">
              <div class="wc-count">{{ t.count }} 字</div>
              <div class="wc-label">{{ t.label }}</div>
            </div>
            <div class="wc-desc">{{ t.desc }}</div>
          </div>
        </div>

        <!-- 自定义 -->
        <div v-else class="wc-custom">
          <div class="wc-custom-display">{{ customWordCount }} 字</div>
          <input
            v-model="customWordCount"
            type="number"
            class="wc-custom-input"
            min="1"
            max="3000"
            placeholder="输入 1-3000 字"
          />
          <input
            v-model="customWordCount"
            type="range"
            class="wc-slider"
            min="1"
            max="3000"
          />
          <div class="wc-custom-hint">AI 将生成约 {{ customWordCount }} 字的文章</div>
        </div>
      </div>
    </a-modal>

    <!-- 风格选择弹框 -->
    <a-modal
      v-model:open="styleVisible"
      :footer="null"
      :width="720"
      centered
    >
      <template #title>
        <div class="modal-title-wrap">
          <div class="modal-title">风格库</div>
          <div class="modal-subtitle">选择一套预设风格，让 AI 写出你想要的调性</div>
        </div>
      </template>

      <!-- 创建/编辑风格 -->
      <div v-if="createStyleMode" class="style-editor">
        <div class="style-editor-header">
          <button class="style-editor-back" @click="goBackToList">← 返回</button>
          <div class="style-editor-title">{{ editingStyle.name ? '编辑提示词' : '新建我的风格' }}</div>
        </div>
        <div class="style-editor-form">
          <div class="style-editor-field">
            <label class="style-editor-label">风格名称 <span class="required">*</span></label>
            <input
              v-model="editingStyle.name"
              type="text"
              class="style-editor-input"
              placeholder="例如：我的小红书风"
              maxlength="20"
            />
          </div>
          <div class="style-editor-field">
            <label class="style-editor-label">风格提示词 <span class="required">*</span></label>
            <textarea
              v-model="editingStyle.prompt"
              class="style-editor-textarea"
              placeholder="描述你希望 AI 采用的语气、结构、用词习惯等..."
              rows="5"
            ></textarea>
            <div class="style-editor-hint">提示词会作为系统提示的一部分影响生成结果。</div>
          </div>
          <div class="style-editor-field">
            <label class="style-editor-label">适用范围 <span class="required">*</span></label>
            <input
              v-model="editingStyle.scope"
              type="text"
              class="style-editor-input"
              placeholder="例：公众号情感文 / 产品评测 / 小红书种草"
              maxlength="50"
            />
          </div>
          <div class="style-editor-presets">
            <div class="style-editor-preset-label">快速填充模板：</div>
            <div class="style-editor-preset-list">
              <div
                v-for="preset in stylePresets"
                :key="preset.name"
                class="style-preset-card"
                @click="editingStyle.prompt = preset.prompt"
              >
                <div class="style-preset-title">{{ preset.name }}</div>
                <div class="style-preset-desc">{{ preset.desc }}</div>
              </div>
            </div>
          </div>
          <button class="save-style-btn" @click="saveStyle">保存</button>
        </div>
      </div>

      <!-- 风格列表 -->
      <template v-else>
        <div class="style-tabs">
          <button
            :class="['style-tab', { active: styleTab === 'my' }]"
            @click="styleTab = 'my'; createStyleMode = false"
          >
            我的风格
          </button>
          <button
            :class="['style-tab', { active: styleTab === 'learned' }]"
            @click="styleTab = 'learned'; createStyleMode = false"
          >
            学习的风格
          </button>
          <button
            :class="['style-tab', { active: styleTab === 'system' }]"
            @click="styleTab = 'system'; createStyleMode = false"
          >
            系统预设风格
          </button>
        </div>

        <div class="style-content">
          <!-- 系统预设 -->
          <div v-show="styleTab === 'system'" class="style-grid">
            <div
              v-for="s in systemStyles"
              :key="s.name"
              :class="['style-card', { selected: selectedStyleName === s.name }]"
              @click="selectStyle(s)"
            >
              <div class="style-card-title">{{ s.name }}</div>
              <div class="style-card-desc">{{ s.desc }}</div>
              <div class="style-card-prompt">{{ s.promptSummary }}</div>
            </div>
          </div>

          <!-- 我的风格 -->
          <div v-show="styleTab === 'my'" class="style-grid">
            <div class="style-add-card" @click="goToCreateStyle">
              <div class="style-add-icon">+</div>
              <div class="style-add-text">新建我的风格</div>
            </div>
            <div
              v-for="(m, idx) in myStyles"
              :key="m.name"
              :class="['style-card', { selected: selectedStyleName === m.name }]"
              @click="selectStyle(m)"
            >
              <div class="style-card-title">{{ m.name }}</div>
              <div class="style-card-desc">{{ m.desc }} · 已用 {{ m.count }} 次</div>
              <div v-if="m.scope" class="style-card-scope">适用：{{ m.scope }}</div>
              <div class="style-prompt-toggle" @click.stop="togglePrompt(idx)">
                {{ expandedPromptIdx === idx ? '收起 ▴' : '查看完整提示词 ▾' }}
              </div>
              <div v-show="expandedPromptIdx === idx" class="style-prompt-full">
                {{ m.prompt }}
              </div>
              <div v-show="expandedPromptIdx === idx" class="style-prompt-actions">
                <button class="style-action-btn" @click.stop="goToEditStyle(m)">编辑提示词</button>
                <button class="style-action-btn style-del-btn" @click.stop="deleteStyle(m.name)">删除</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 学习的风格 -->
        <div v-show="styleTab === 'learned'" class="style-grid">
          <div
            v-if="learnedStyles.length === 0"
            class="style-empty"
          >
            还没有学习过的风格，请前往「我的风格」页面学习。
          </div>
          <div
            v-for="(l, idx) in learnedStyles"
            v-else
            :key="l.name"
            :class="['style-card', { selected: selectedStyleName === l.name }]"
            @click="selectStyle(l)"
          >
            <div class="style-card-title">{{ l.name }}</div>
            <div v-if="l.scope" class="style-card-scope">适用：{{ l.scope }}</div>
            <div class="style-prompt-toggle" @click.stop="toggleLearnedPrompt(idx)">
              {{ expandedLearnedIdx === idx ? '收起 ▴' : '查看完整提示词 ▾' }}
            </div>
            <div v-show="expandedLearnedIdx === idx" class="style-prompt-full">
              {{ l.prompt }}
            </div>
          </div>
        </div>

        <div class="style-footer">
          <button
            class="style-apply-btn"
            :disabled="!selectedStyleName"
            @click="applyStyle"
          >
            应用
          </button>
        </div>
      </template>
    </a-modal>

    <!-- 草稿箱弹框 -->
    <a-modal
      v-model:open="draftBoxVisible"
      title="草稿箱"
      :footer="null"
      :width="480"
      centered
    >
      <div :key="draftBoxKey">
        <div v-if="draftList.length === 0" class="draft-box-empty">
          <a-empty description="草稿箱是空的" />
        </div>
        <div v-else class="draft-box-list">
          <div
            v-for="draft in draftList"
            :key="draft.id"
            class="draft-item"
            @click="restoreDraft(draft)"
          >
            <div class="draft-info">
              <div class="draft-title">{{ draft.customTitle || '未命名草稿' }}</div>
              <div class="draft-desc">{{ draft.customRequirement || '未填写要求' }}</div>
              <div class="draft-meta">
                <span>{{ draft.platform?.name || '未选择平台' }}</span>
                <span>·</span>
                <span>{{ draft.wordCount?.count || 0 }}字</span>
                <span>·</span>
                <span>{{ formatDate(draft.savedAt) }}</span>
              </div>
            </div>
            <button class="draft-del-btn" @click="deleteDraft(draft.id, $event)">删除</button>
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 模板选择弹框 -->
    <a-modal
      v-model:open="templateVisible"
      :footer="null"
      :width="960"
      centered
      :closable="true"
      class="template-lib-modal"
    >
      <template #title>
        <div class="modal-title-wrap">
          <div class="modal-title">导出模板库</div>
          <div class="modal-subtitle">共 {{ filteredTemplates.length }} 个模板 · 左侧实时预览 · 右侧选择模板</div>
        </div>
      </template>

      <!-- 平台标签 -->
      <div class="template-tabs">
        <button
          v-for="tab in templatePlatformTabs"
          :key="tab.key"
          :class="['template-tab', { active: templatePlatformTab === tab.key }]"
          @click="templatePlatformTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- 2列布局 -->
      <div class="template-body">
        <!-- 左侧：大预览 -->
        <div class="template-preview-pane" v-html="currentTemplatePreview"></div>

        <!-- 右侧：模板列表 -->
        <div class="template-list-pane">
          <div
            v-for="t in filteredTemplates"
            :key="t.key"
            :class="['template-row', { selected: selectedTemplateKey === t.key }]"
            @click="selectTemplate(t)"
          >
            <div class="template-row-info">
              <div class="template-row-name">{{ t.name }}</div>
              <div class="template-row-desc">{{ t.desc }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="template-footer">
        <button class="template-apply-btn" @click="applyTemplate">应用</button>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { FolderOutlined, LoadingOutlined, CheckCircleOutlined, ClockCircleOutlined, InboxOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import {
  systemStyles,
  myStyles,
  currentStyle,
  applyStyle as applyStyleShared,
  addCustomStyle,
  updateCustomStyle,
  removeCustomStyle,
  learnedStyles
} from '@/composables/useStyles.js'
import { marketStyles } from '@/composables/useStyleMarket.js'

const router = useRouter()
const route = useRoute()

// 恢复草稿（加载最新一个）
onMounted(() => {
  const drafts = JSON.parse(localStorage.getItem('aichuangzuo_drafts') || '[]')
  if (drafts.length > 0) {
    const data = drafts[0]
    customTitle.value = data.customTitle || ''
    customRequirement.value = data.customRequirement || ''
    if (data.platform) {
      const p = platforms.find(x => x.key === data.platform.key)
      if (p) currentPlatform.value = p
    }
    if (data.wordCount) currentWordCount.value = data.wordCount
    if (data.style) currentStyle.value = data.style
    if (data.template) currentTemplate.value = data.template
  }

  // 从风格市场跳转过来时自动应用风格
  const marketStyleId = route.query.marketStyleId
  if (marketStyleId) {
    const s = marketStyles.value.find(x => x.id === marketStyleId)
    if (s) {
      applyStyleShared({
        name: s.name,
        prompt: s.prompt,
        scope: s.scope
      })
      selectedStyleName.value = s.name
      router.replace({ path: route.path })
    }
  }

  loadMiniQueue()
  // 定时刷新队列
  setInterval(loadMiniQueue, 2000)
})

// 加载简化队列数据
const miniQueueList = ref([])

const loadMiniQueue = () => {
  const saved = localStorage.getItem('aichuangzuo_generation_queue')
  if (saved) {
    try {
      const list = JSON.parse(saved)
      // 排序：generating > queued > completed
      const sorted = list.sort((a, b) => {
        const order = { generating: 0, queued: 1, completed: 2, failed: 3 }
        return order[a.status] - order[b.status]
      })
      miniQueueList.value = sorted.slice(0, 20)

      // 恢复正在生成的任务进度
      list.forEach(item => {
        if (item.status === 'generating') {
          continueGeneration(item.id)
        }
      })
    } catch (e) {
      miniQueueList.value = []
    }
  } else {
    miniQueueList.value = []
  }
}

// 继续模拟生成
const continueGeneration = (id) => {
  const interval = setInterval(() => {
    const current = miniQueueList.value.find(x => x.id === id)
    if (!current || current.status !== 'generating') {
      clearInterval(interval)
      return
    }
    current.progress = Math.min(100, Math.round((current.progress + Math.random() * 15 + 5) * 10) / 10)
    if (current.progress >= 100) {
      current.progress = 100
      current.status = 'completed'
      current.completedAt = new Date().toISOString()
      current.content = generateMockContent(current)
      saveMiniQueue()
      clearInterval(interval)
    } else {
      saveMiniQueue()
    }
  }, 500)
}

const miniStatusText = (status) => {
  const map = { generating: '生成中', queued: '排队中', completed: '已完成', failed: '失败' }
  return map[status] || status
}

// 额度
const quotaTotal = ref(100)
const quotaRemaining = ref(88)
const quotaUsed = computed(() => quotaTotal.value - quotaRemaining.value)
const usedPercent = computed(() => Math.round((quotaUsed.value / quotaTotal.value) * 100))
const quotaColor = computed(() => {
  const used = quotaUsed.value / quotaTotal.value
  if (used < 0.6) return '#07c160'  // 绿色
  if (used < 0.9) return '#faad14'  // 黄色
  return '#ff4d4f'  // 红色
})

// 灵感选题
const topics = ref([
  { id: 1, title: '工作 3 年没升职？可能是这 3 个习惯在拖后腿', tag: '职场效率', heat: '3.2k', used: false },
  { id: 2, title: '我用 AI 写作月入过万：新手可复制的 5 个步骤', tag: 'AI 副业', heat: '5.8k', used: false },
  { id: 3, title: '为什么你越努力越焦虑？3 个思维陷阱正在消耗你', tag: '情感成长', heat: '4.1k', used: false },
  { id: 4, title: '月薪 5000 如何一年存下 3 万？我的省钱清单公开', tag: '生活技巧', heat: '6.5k', used: false },
  { id: 5, title: '30 岁后才明白：真正成熟的人，都懂得边界感', tag: '情感成长', heat: '2.9k', used: false },
  { id: 6, title: 'AI 时代，普通人如何抓住新机会的 4 个思路', tag: 'AI 副业', heat: '7.2k', used: false }
])

const applyTopic = (topic) => {
  customTitle.value = topic.title
  if (customRequirement.value.trim()) {
    customRequirement.value = customRequirement.value.trim() + '\n\n基于选题：' + topic.title
  } else {
    customRequirement.value = '请围绕这个选题生成一篇文章。'
  }
  topic.used = true
}

const refreshTopics = () => {
  // 模拟换一批
  const shuffled = [...topics.value].sort(() => Math.random() - 0.5)
  topics.value = shuffled
}

// 创作内容
const customTitle = ref('')
const customRequirement = ref('')

// 平台
const platforms = [
  { key: 'wechat', name: '公众号', desc: '深度长文，适合专业内容输出' },
  { key: 'xiaohongshu', name: '小红书', desc: '轻松图文，种草安利效果好' },
  { key: 'toutiao', name: '今日头条', desc: '算法分发，热点资讯类内容' },
  { key: 'baijiahao', name: '百家号', desc: '多平台分发，SEO友好' },
  { key: 'douyin', name: '抖音图文', desc: '短视频+图文，流量大' },
  { key: 'zhihu', name: '知乎', desc: '深度问答，专业知识分享' }
]
const currentPlatform = ref(platforms[0])
const platformVisible = ref(false)

const openPlatformModal = () => {
  platformVisible.value = true
}

const selectPlatform = (p) => {
  currentPlatform.value = p
  platformVisible.value = false
}

// 字数
const wordCountTab = ref('tier')
const wordCountPresets = {
  platform: {
    wechat: [
      { count: 800, label: '早报 / 简评' },
      { count: 1500, label: '标准深度文' },
      { count: 2500, label: '专题报道' },
      { count: 3000, label: '行业研究（上限）' }
    ],
    xiaohongshu: [
      { count: 300, label: '标题种草' },
      { count: 500, label: '图文分享' },
      { count: 800, label: '详细测评' },
      { count: 1200, label: '步骤拆解教程' }
    ],
    toutiao: [
      { count: 400, label: '热点快讯' },
      { count: 800, label: '事件报道' },
      { count: 1500, label: '专题分析' },
      { count: 2000, label: '观点长文' }
    ],
    baijiahao: [
      { count: 1000, label: '知识科普' },
      { count: 1500, label: '生活攻略' },
      { count: 2000, label: '人文叙事' },
      { count: 2500, label: '行业洞察' }
    ],
    douyin: [
      { count: 150, label: '封面金句' },
      { count: 300, label: '图配文' },
      { count: 600, label: '情感短篇' }
    ],
    general: [
      { count: 500, label: '短文' },
      { count: 1000, label: '中等' },
      { count: 1500, label: '标准' },
      { count: 2500, label: '长文' }
    ]
  },
  scenario: [
    { count: 1200, label: '教程 / 步骤', desc: '操作步骤详细说明，适合图文对照' },
    { count: 1000, label: '测评 / 对比', desc: '优缺点详细对比，附评分' },
    { count: 500, label: '清单 / 种草', desc: '快速清单 + 标签，重点突出' },
    { count: 1800, label: '故事 / 叙事', desc: '沉浸式叙事，节奏完整' }
  ],
  tier: [
    { count: 500, label: '短文', desc: '速读，3 分钟读完' },
    { count: 1000, label: '中等', desc: '快速浏览，5 分钟读完' },
    { count: 1500, label: '标准', desc: '深度阅读，8 分钟读完' },
    { count: 2500, label: '长文', desc: '完整报告，12 分钟读完' }
  ]
}
const customWordCount = ref(1500)
const currentWordCount = ref({ count: 1500, label: '标准', desc: '深度阅读，8 分钟读完' })
const wordCountVisible = ref(false)

const wordCountTabs = [
  { key: 'platform', label: '按平台推荐' },
  { key: 'scenario', label: '按内容场景' },
  { key: 'tier', label: '按字数档位' },
  { key: 'custom', label: '自定义字数' }
]

const platformWordCounts = computed(() => {
  const platform = currentPlatform.value?.key || 'wechat'
  return wordCountPresets.platform[platform] || wordCountPresets.platform.general
})

const openWordCountModal = () => {
  wordCountVisible.value = true
}

const selectWordCount = (wc) => {
  currentWordCount.value = wc
  wordCountVisible.value = false
}

// 风格
const styleTab = ref('my')
const styleVisible = ref(false)
const selectedStyleName = ref(null)
const expandedPromptIdx = ref(null)
const expandedLearnedIdx = ref(null)
const createStyleMode = ref(false)
const editingStyle = reactive({ originalName: '', name: '', prompt: '', scope: '', isEdit: false })

const openStyleModal = () => {
  styleTab.value = 'my'
  selectedStyleName.value = null
  expandedPromptIdx.value = null
  createStyleMode.value = false
  styleVisible.value = true
}

const selectStyle = (s) => {
  selectedStyleName.value = s.name
}

const applyStyle = () => {
  if (!selectedStyleName.value) return
  const s = systemStyles.find(x => x.name === selectedStyleName.value) ||
            myStyles.value.find(x => x.name === selectedStyleName.value)
  if (s) {
    applyStyleShared(s)
    styleVisible.value = false
  }
}

const goToCreateStyle = () => {
  createStyleMode.value = true
  editingStyle.isEdit = false
  editingStyle.originalName = ''
  editingStyle.name = ''
  editingStyle.prompt = ''
  editingStyle.scope = ''
}

const goToEditStyle = (style) => {
  createStyleMode.value = true
  editingStyle.isEdit = true
  editingStyle.originalName = style.name
  editingStyle.name = style.name
  editingStyle.prompt = style.prompt
  editingStyle.scope = style.scope || ''
}

const goBackToList = () => {
  createStyleMode.value = false
}

const saveStyle = () => {
  const name = editingStyle.name.trim()
  const prompt = editingStyle.prompt.trim()
  const scope = editingStyle.scope.trim()
  if (!name || !prompt || !scope) return
  if (name.length > 20 || prompt.length > 1000 || scope.length > 50) return
  if (editingStyle.isEdit) {
    updateCustomStyle(editingStyle.originalName, {
      name,
      prompt,
      scope
    })
  } else {
    addCustomStyle({
      name,
      prompt,
      scope
    })
  }
  createStyleMode.value = false
}

const deleteStyle = (name) => {
  removeCustomStyle(name)
  if (selectedStyleName.value === name) selectedStyleName.value = null
}

const togglePrompt = (idx) => {
  expandedPromptIdx.value = expandedPromptIdx.value === idx ? null : idx
}

const toggleLearnedPrompt = (idx) => {
  expandedLearnedIdx.value = expandedLearnedIdx.value === idx ? null : idx
}

const stylePresets = [
  { name: '产品评测', desc: '客观中立、参数对比', prompt: '你是客观的产品评测人：\n- 语气客观中立、有理有据\n- 结构：外观设计 → 核心性能 → 实际体验 → 优缺点总结\n- 必带参数对比表\n- 给出明确购买建议' },
  { name: '情感散文', desc: '细腻温暖、意象留白', prompt: '你是细腻的散文家：\n- 语气细腻、温暖，共情\n- 大量使用比喻、意象、留白\n- 第一人称叙述\n- 段落短而精，不要说教' },
  { name: '职场干货', desc: '专业务实、可执行', prompt: '你是资深职场导师：\n- 语气专业务实\n- 结构：行业痛点 → 核心方案 → 具体步骤\n- 必带可执行的 checklist\n- 避免假大空、避免鸡汤' },
  { name: '营销文案', desc: '紧迫感 + 利益点', prompt: '你是营销高手：\n- 开头制造紧迫感 / 共鸣痛点\n- 突出 3 个核心利益点\n- 必带强 CTA\n- 语气坚定、有说服力' }
]

// 模板
const templatePlatformTabs = [
  { key: 'all', label: '全部' },
  { key: 'wechat', label: '公众号' },
  { key: 'xiaohongshu', label: '小红书' },
  { key: 'toutiao', label: '今日头条' },
  { key: 'baijiahao', label: '百家号' },
  { key: 'zhihu', label: '知乎' },
  { key: 'douyin', label: '抖音图文' },
  { key: 'general', label: '通用风格' }
]
const templatePlatformTab = ref('all')

const allTemplates = [
  { key: 'wechat', name: '公众号标准模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '简洁专业，适合深度长文' },
  { key: 'business', name: '商业财经模板', bgColor: '#fff', textColor: '#003a8c', desc: '专业严谨，适合商业分析' },
  { key: 'marketing', name: '营销推广模板', bgColor: '#fff', textColor: '#cf1322', desc: '吸引眼球，适合营销内容' },
  { key: 'academic', name: '学术论文模板', bgColor: '#fafaf5', textColor: '#1a1a1a', desc: '严谨规范，适合学术写作' },
  { key: 'toutiao', name: '头条号标准模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '算法友好，适合热点资讯' },
  { key: 'xiaohongshu', name: '小红书爆款模板', bgColor: '#fff', textColor: '#ff2442', desc: '种草安利，适合小红书风格' },
  { key: 'baijiahao', name: '百家号模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '多平台分发，适合资讯类' },
  { key: 'story', name: '故事叙事模板', bgColor: '#faf5ef', textColor: '#8b5e34', desc: '沉浸叙事，适合故事类内容' },
  { key: 'magazine', name: '杂志风模板', bgColor: '#fafafa', textColor: '#1a1a1a', desc: '精致排版，适合生活方式' },
  { key: 'card', name: '卡片式模板', bgColor: '#f5f5f5', textColor: '#1a1a1a', desc: '模块化展示，信息清晰' },
  { key: 'checklist', name: '清单攻略模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '步骤清晰，适合教程攻略' },
  { key: 'dark', name: '深色模式模板', bgColor: '#1a1a1a', textColor: '#fff', desc: '科技感强，适合技术内容' },
  { key: 'wechat-minimal', name: '公众号简约模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '简约留白，适合文艺内容' },
  { key: 'wechat-dialogue', name: '公众号对话模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '对话形式，适合访谈采访' },
  { key: 'wechat-brand', name: '品牌故事模板', bgColor: '#fff', textColor: '#07c160', desc: '品牌调性，适合企业宣传' },
  { key: 'wechat-infographic', name: '信息图模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '数据可视化，适合信息图解' },
  { key: 'xiaohongshu-list', name: '小红书清单模板', bgColor: '#fff', textColor: '#ff2442', desc: '清单形式，适合好物推荐' },
  { key: 'xiaohongshu-review', name: '小红书测评模板', bgColor: '#fff', textColor: '#ff2442', desc: '测评对比，适合产品体验' },
  { key: 'xiaohongshu-tutorial', name: '小红书教程模板', bgColor: '#fff', textColor: '#ff2442', desc: '步骤详细，适合教程分享' },
  { key: 'xiaohongshu-emotion', name: '小红书情感模板', bgColor: '#fff', textColor: '#ff2442', desc: '情感共鸣，适合成长分享' },
  { key: 'toutiao-news', name: '头条新闻模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '新闻体，适合资讯报道' },
  { key: 'toutiao-depth', name: '头条深度模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '深度分析，适合专题报道' },
  { key: 'toutiao-hot', name: '头条热点模板', bgColor: '#fff', textColor: '#ff6600', desc: '热点追踪，适合爆款文章' },
  { key: 'toutiao-qa', name: '头条问答模板', bgColor: '#fff', textColor: '#ff6600', desc: '问答形式，适合知识解答' },
  { key: 'baijiahao-science', name: '百家号科普模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '知识科普，适合科学讲解' },
  { key: 'baijiahao-history', name: '百家号历史模板', bgColor: '#fafafa', textColor: '#1a1a1a', desc: '人文历史，适合故事讲述' },
  { key: 'baijiahao-guide', name: '百家号攻略模板', bgColor: '#fff', textColor: '#1677ff', desc: '实用攻略，适合生活指南' },
  { key: 'douyin-graphic', name: '抖音图文金句模板', bgColor: '#f5f5f5', textColor: '#1a1a1a', desc: '金句卡片，适合短视频配套' },
  { key: 'douyin-quote', name: '抖音图文语录模板', bgColor: '#f5f5f5', textColor: '#1a1a1a', desc: '语录风格，适合励志文案' },
  { key: 'zhihu-answer', name: '知乎回答模板', bgColor: '#fff', textColor: '#1a1a1a', desc: '专业回答，适合知识分享' }
]

const filteredTemplates = computed(() => {
  if (templatePlatformTab.value === 'all') return allTemplates
  if (templatePlatformTab.value === 'general') return allTemplates.filter(t => ['business', 'marketing', 'academic', 'story', 'magazine', 'card', 'checklist', 'dark'].includes(t.key))
  return allTemplates.filter(t => t.key.startsWith(templatePlatformTab.value))
})

const selectedTemplateKey = ref('wechat')
const currentTemplate = computed(() => allTemplates.find(t => t.key === selectedTemplateKey.value) || allTemplates[0])
const templateVisible = ref(false)
const draftBoxVisible = ref(false)
const draftBoxKey = ref(0)

watch(draftBoxVisible, (val) => {
  if (val) draftBoxKey.value++
})

// 草稿列表
const draftList = computed(() => {
  const list = JSON.parse(localStorage.getItem('aichuangzuo_drafts') || '[]')
  console.log('draftList computed:', list)
  return list
})

const restoreDraft = (draft) => {
  customTitle.value = draft.customTitle || ''
  customRequirement.value = draft.customRequirement || ''
  if (draft.platform) {
    const p = platforms.find(x => x.key === draft.platform.key)
    if (p) currentPlatform.value = p
  }
  if (draft.wordCount) currentWordCount.value = draft.wordCount
  if (draft.style) currentStyle.value = draft.style
  if (draft.template) currentTemplate.value = draft.template
  draftBoxVisible.value = false
  message.success('已恢复草稿')
}

const deleteDraft = (id, e) => {
  e.stopPropagation()
  const drafts = JSON.parse(localStorage.getItem('aichuangzuo_drafts') || '[]')
  const idx = drafts.findIndex(d => d.id === id)
  if (idx > -1) {
    drafts.splice(idx, 1)
    localStorage.setItem('aichuangzuo_drafts', JSON.stringify(drafts))
  }
  draftBoxKey.value++
}

// 预览HTML生成
const templateLargeStyles = {
  wechat:    { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', calloutBg: '#f6ffed', calloutBorder: '4px solid #07c160', calloutColor: '#262626' },
  business:  { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#003a8c', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#d6e4ff', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#003a8c', headingSize: '16px', headingBorder: '3px solid #003a8c', headingPl: '10px', calloutBg: '#f0f5ff', calloutBorder: '3px solid #1677ff', calloutColor: '#003a8c' },
  marketing: { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#cf1322', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#ffccc7', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.85', headingColor: '#cf1322', headingSize: '17px', headingBorder: '3px solid #cf1322', headingPl: '10px', calloutVariant: 'cta' },
  academic:  { bg: '#fafaf5', font: 'Georgia, "Songti SC", serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.9', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', calloutBg: '#f5f5f0', calloutBorder: 'none', calloutColor: '#595959', numbered: true },
  toutiao:   { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '15px', bodyLine: '1.9', headingColor: '#ff6600', headingSize: '17px', headingBorder: '4px solid #ff6600', headingPl: '10px', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
  xiaohongshu:{ bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff2442', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'pill' },
  baijiahao: { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#bae0ff', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#1677ff', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingBorderBottom: '2px solid #1677ff', calloutBg: '#e6f4ff', calloutBorder: '3px solid #1677ff', calloutColor: '#1677ff' },
  story:     { bg: '#faf5ef', font: 'Georgia, "Songti SC", serif', titleColor: '#8b5e34', titleSize: '22px', metaColor: '#a89378', metaBorder: '#e8dccb', bodyColor: '#3a2e22', bodySize: '15px', bodyLine: '1.95', headingColor: '#8b5e34', headingSize: '16px', headingBorder: '3px solid #d4a373', headingPl: '10px', calloutBg: '#f0e6d8', calloutBorder: 'none', calloutColor: '#8b5e34' },
  magazine:  { bg: '#fafafa', font: 'Georgia, "Songti SC", serif', titleColor: '#1a1a1a', titleSize: '26px', titleAlign: 'center', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', metaAlign: 'center', bodyColor: '#595959', bodySize: '14px', bodyLine: '1.95', bodyAlign: 'center', headingColor: '#1a1a1a', headingSize: '17px', headingBorder: 'none', headingPl: '0', headingAlign: 'center', calloutBg: '#f5f5f5', calloutBorder: 'none', calloutColor: '#262626' },
  card:      { bg: '#f5f5f5', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '20px', metaColor: '#8c8c8c', metaBorder: '#e8e8e8', bodyColor: '#262626', bodySize: '13px', bodyLine: '1.7', headingColor: '#07c160', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'card' },
  checklist: { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#07c160', headingSize: '16px', headingBorder: 'none', headingPl: '0', calloutVariant: 'checklist' },
  dark:      { bg: '#1a1a1a', font: '-apple-system, sans-serif', titleColor: '#fff', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#333', bodyColor: '#d9d9d9', bodySize: '14px', bodyLine: '1.85', headingColor: '#95de64', headingSize: '16px', headingBorder: '3px solid #95de64', headingPl: '10px', calloutBg: '#262626', calloutBorder: 'none', calloutColor: '#95de64' },
  'wechat-minimal': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.9', headingColor: '#1a1a1a', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutBg: '#fafafa', calloutBorder: '1px solid #e8e8e8', calloutColor: '#595959' },
  'wechat-dialogue': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '20px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#07c160', headingSize: '14px', headingBorder: 'none', headingPl: '0', calloutVariant: 'pill' },
  'wechat-brand': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#07c160', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#d9f7be', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.85', headingColor: '#07c160', headingSize: '16px', headingBorder: '4px solid #07c160', headingPl: '10px', calloutBg: '#f6ffed', calloutBorder: '3px solid #07c160', calloutColor: '#07c160' },
  'wechat-infographic': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.75', headingColor: '#07c160', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'card' },
  'xiaohongshu-list': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff2442', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'checklist' },
  'xiaohongshu-review': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff6600', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
  'xiaohongshu-tutorial': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff2442', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'pill' },
  'xiaohongshu-emotion': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.9', headingColor: '#ff2442', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutBg: '#fff0f3', calloutBorder: '1px solid #ffd1d9', calloutColor: '#262626' },
  'toutiao-news': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '15px', bodyLine: '1.9', headingColor: '#ff6600', headingSize: '17px', headingBorder: '4px solid #ff6600', headingPl: '10px', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
  'toutiao-depth': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '15px', bodyLine: '1.85', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingBorderBottom: '2px solid #ff6600', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
  'toutiao-hot': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff6600', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '15px', bodyLine: '1.9', headingColor: '#ff6600', headingSize: '17px', headingBorder: '4px solid #ff6600', headingPl: '10px', calloutVariant: 'cta' },
  'toutiao-qa': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff6600', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff6600', headingSize: '16px', headingBorder: '4px solid #ff6600', headingPl: '10px', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
  'baijiahao-science': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#bae0ff', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#1677ff', headingSize: '16px', headingBorder: '4px solid #1677ff', headingPl: '10px', calloutBg: '#e6f4ff', calloutBorder: '3px solid #1677ff', calloutColor: '#1677ff' },
  'baijiahao-history': { bg: '#fafafa', font: 'Georgia, "Songti SC", serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.9', headingColor: '#1677ff', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingBorderBottom: '1px solid #1677ff', calloutBg: '#e6f4ff', calloutBorder: '3px solid #1677ff', calloutColor: '#1677ff' },
  'baijiahao-guide': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1677ff', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#bae0ff', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.75', headingColor: '#1677ff', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'card' },
  'douyin-graphic': { bg: '#f5f5f5', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '26px', titleAlign: 'center', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', metaAlign: 'center', bodyColor: '#1a1a1a', bodySize: '15px', bodyLine: '1.75', bodyAlign: 'center', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingAlign: 'center', calloutBg: '#fff', calloutBorder: '1px solid #d9d9d9', calloutColor: '#1a1a1a' },
  'douyin-quote': { bg: '#f5f5f5', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '24px', titleAlign: 'center', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', metaAlign: 'center', bodyColor: '#1a1a1a', bodySize: '16px', bodyLine: '1.65', bodyAlign: 'center', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingAlign: 'center', calloutBg: '#1a1a1a', calloutBorder: 'none', calloutColor: '#fff' },
  'zhihu-answer': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.85', headingColor: '#0066ff', headingSize: '16px', headingBorder: '4px solid #0066ff', headingPl: '10px', calloutBg: '#f0f5ff', calloutBorder: '3px solid #0066ff', calloutColor: '#0066ff' }
}

function buildLargePreview(t) {
  const s = templateLargeStyles[t.key] || templateLargeStyles.wechat
  const titleAlign = s.titleAlign || 'left'
  const metaAlign = s.metaAlign || 'left'
  const bodyAlign = s.bodyAlign || 'left'
  const headingAlign = s.headingAlign || 'left'
  const headingPl = s.headingPl || '0'
  const headingBorderBottom = s.headingBorderBottom || 'none'

  let calloutHtml
  if (s.calloutVariant === 'cta') {
    calloutHtml = '<div style="background: #fff; padding: 12px 14px; color: #262626; font-size: 13px; line-height: 1.6; border-radius: 6px; margin-top: 14px; border: 2px solid #cf1322; text-align: center;"><strong style="color: #cf1322;">限时优惠</strong> · 立即行动 · 别错过</div>'
  } else if (s.calloutVariant === 'pill') {
    calloutHtml = '<div style="background: #fff0f2; padding: 8px 14px; color: #ff2442; font-size: 13px; line-height: 1.6; border-radius: 20px; margin-top: 14px; display: inline-block;"><strong>核心要点：</strong>高效管理时间就是管理注意力</div>'
  } else if (s.calloutVariant === 'card') {
    calloutHtml = '<div style="background: #fff; padding: 12px 14px; color: #262626; font-size: 13px; line-height: 1.6; border-radius: 8px; margin-top: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); border-left: 3px solid #07c160;"><strong style="color:#07c160;">关键结论：</strong>管理时间本质是管理注意力。</div>'
  } else if (s.calloutVariant === 'checklist') {
    calloutHtml = '<div style="background: #f6ffed; padding: 12px 14px; color: #262626; font-size: 13px; line-height: 1.9; border-radius: 6px; margin-top: 14px;"><div style="color: #07c160; font-weight: 500;">✓ 列出今日最重要的 3 件事</div><div style="color: #07c160; font-weight: 500;">✓ 先完成最难的那一件</div><div style="color: #07c160; font-weight: 500;">✓ 时间块专注单线程</div></div>'
  } else {
    const borderStyle = s.calloutBorder === 'none' ? 'border: none;' : ('border-left: ' + s.calloutBorder + ';')
    calloutHtml = '<div style="background: ' + s.calloutBg + '; ' + borderStyle + ' padding: 12px 14px; color: ' + s.calloutColor + '; font-size: 13px; line-height: 1.6; border-radius: 0 6px 6px 0; margin-top: 14px;"><strong style="color:#1a1a1a;">关键结论：</strong>管理时间本质是管理注意力。</div>'
  }

  const headingStyleExtra = (headingBorderBottom !== 'none') ? ('padding-bottom: 6px; ') : ''
  const headingText = s.numbered ? '一、优先级排序：先做重要的事' : '01｜优先级排序：先做重要的事'

  return '<div style="background: ' + s.bg + '; padding: 24px; height: 100%; box-sizing: border-box; font-family: ' + s.font + '; overflow-y: auto; color: ' + s.bodyColor + ';">' +
    '<h1 style="font-size: ' + s.titleSize + '; font-weight: 700; color: ' + s.titleColor + '; margin: 0 0 12px; line-height: 1.4; text-align: ' + titleAlign + ';">如何高效管理时间</h1>' +
    '<div style="color: ' + s.metaColor + '; font-size: 12px; margin-bottom: 16px; padding-bottom: 10px; border-bottom: 1px solid ' + s.metaBorder + '; text-align: ' + metaAlign + ';">2026-06-22 · 约 1500 字 · 风格:专业严谨</div>' +
    '<p style="font-size: ' + s.bodySize + '; line-height: ' + s.bodyLine + '; color: ' + s.bodyColor + '; margin: 0 0 12px; text-align: ' + bodyAlign + ';">时间对每个人来说都是公平的，但为什么有人能在 24 小时内完成更多事情？关键不在于你有多忙，而在于你如何管理注意力。</p>' +
    '<h3 style="font-size: ' + s.headingSize + '; font-weight: 600; color: ' + s.headingColor + '; border-left: ' + s.headingBorder + '; padding-left: ' + headingPl + '; border-bottom: ' + headingBorderBottom + '; ' + headingStyleExtra + 'margin: 18px 0 8px; text-align: ' + headingAlign + ';">' + headingText + '</h3>' +
    '<p style="font-size: ' + s.bodySize + '; line-height: ' + s.bodyLine + '; color: ' + s.bodyColor + '; margin: 0 0 12px; text-align: ' + bodyAlign + ';">很多人一早打开手机就被消息牵着走。高效的人会在每天开始前列出 3 件最重要的事，并优先完成它们。</p>' +
    calloutHtml +
    '</div>'
}

const currentTemplatePreview = computed(() => buildLargePreview(currentTemplate.value))

const openTemplateModal = () => {
  selectedTemplateKey.value = currentTemplate.value.key
  templateVisible.value = true
}

const selectTemplate = (t) => {
  selectedTemplateKey.value = t.key
}

const applyTemplate = () => {
  currentTemplate.value = allTemplates.find(t => t.key === selectedTemplateKey.value) || allTemplates[0]
  templateVisible.value = false
}

const formatDate = (dateStr) => {
  const d = new Date(dateStr)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hour = d.getHours().toString().padStart(2, '0')
  const min = d.getMinutes().toString().padStart(2, '0')
  return `${month}月${day}日 ${hour}:${min}`
}

// 导出弹框
const exportModalVisible = ref(false)
const exportArticle = ref(null)
const exportPublishDesc = ref('')
const exportTags = ref([])
const titleOptVisible = ref(false)
const titleOptSuggestions = ref([])
const selectedOptTitle = ref('')
const cardsModalVisible = ref(false)
const cardsStyle = ref('xiaohongshu')
const cardsData = ref([])
const cardCanvasRefs = ref([])

const openExportModal = (item) => {
  // 从完整队列数据中获取文章内容
  const saved = localStorage.getItem('aichuangzuo_generation_queue')
  if (saved) {
    try {
      const list = JSON.parse(saved)
      const fullItem = list.find(x => x.id === item.id)
      if (fullItem && fullItem.content) {
        exportArticle.value = {
          id: fullItem.id,
          title: fullItem.title,
          body: fullItem.content.body || '',
          wordCount: item.wordCount,
          completedAt: item.completedAt ? formatDate(item.completedAt) : '',
          style: fullItem.style?.name || '专业严谨'
        }
        generateExportMeta()
        exportModalVisible.value = true
      }
    } catch (e) {
      console.error('load export article error', e)
    }
  }
}

const editExportArticle = () => {
  if (!exportArticle.value) return
  localStorage.setItem('aichuangzuo_current_article', JSON.stringify(exportArticle.value))
  exportModalVisible.value = false
  router.push('/console/edit')
}

const generateExportMeta = () => {
  const descOptions = [
    '深度解析时间管理的核心技巧，帮助你从忙碌中解脱出来',
    '学会这5个时间管理方法，让你的效率提升300%',
    '自律从管理时间开始，这篇文章告诉你如何做到'
  ]
  const tagOptions = ['时间管理', '效率提升', '自我管理', '职场成长', '习惯养成', '目标规划']
  exportPublishDesc.value = descOptions[Math.floor(Math.random() * descOptions.length)]
  exportTags.value = tagOptions.sort(() => Math.random() - 0.5).slice(0, 4)
}

const formattedExportBody = computed(() => {
  if (!exportArticle.value?.body) return ''
  return exportArticle.value.body
    .replace(/\n\n/g, '</p><p style="margin-bottom: 16px;">')
    .replace(/\n/g, '<br>')
    .replace(/^/, '<p style="margin-bottom: 16px;">')
    .replace(/$/, '</p>')
    .replace(/【([^】]+)】/g, '<h2 style="font-size: 18px; font-weight: 600; color: #1a1a1a; margin: 24px 0 12px;">$1</h2>')
})

const copyExportText = () => {
  if (!exportArticle.value) return
  const text = `${exportArticle.value.title}\n\n${exportArticle.value.body}`
  navigator.clipboard.writeText(text).then(() => {
    message.success('已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}

const handleExportWord = () => {
  if (!exportArticle.value) return

  const title = exportArticle.value.title || '未命名文章'
  const bodyHtml = formattedExportBody.value

  const html = `
    <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns="http://www.w3.org/1999/xhtml">
      <head>
        <meta charset="UTF-8">
        <title>${title}</title>
      </head>
      <body style="font-family: -apple-system, BlinkMacSystemFont, sans-serif; padding: 40px; color: #262626;">
        <h1 style="font-size: 24px; margin-bottom: 16px; line-height: 1.4; color: #1a1a1a;">${title}</h1>
        <div style="font-size: 16px; line-height: 1.8;">${bodyHtml}</div>
      </body>
    </html>
  `

  const blob = new Blob(['\ufeff', html], { type: 'application/msword' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = title.replace(/[\\\\/:*?"<>|]/g, '_') + '.doc'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)

  message.success('Word 导出成功')
}

const copyExportDesc = () => {
  navigator.clipboard.writeText(exportPublishDesc.value).then(() => {
    message.success('描述已复制')
  }).catch(() => {
    message.error('复制失败')
  })
}

const regenerateExportDesc = () => {
  generateExportMeta()
  message.success('已生成新描述')
}

const copyExportTags = () => {
  navigator.clipboard.writeText(exportTags.value.join(' ')).then(() => {
    message.success('标签已复制')
  }).catch(() => {
    message.error('复制失败')
  })
}

const regenerateExportTags = () => {
  const allTags = ['时间管理', '效率提升', '自我管理', '职场成长', '习惯养成', '目标规划', '专注力', '计划执行']
  exportTags.value = allTags.sort(() => Math.random() - 0.5).slice(0, 4)
  message.success('已生成新标签')
}

const optimizeExportTitle = () => {
  if (!exportArticle.value) return
  titleOptSuggestions.value = generateTitleSuggestions(exportArticle.value.title)
  selectedOptTitle.value = ''
  titleOptVisible.value = true
}

const generateTitleSuggestions = (title) => {
  return [
    `${title}：从入门到精通的完整指南`,
    `为什么${title}？看完这篇你就懂了`,
    `${title}，方法其实很简单`,
    `关于${title}，你必须知道的 5 件事`,
    `${title}：实测有效的经验分享`
  ]
}

const selectOptTitle = (title) => {
  selectedOptTitle.value = title
}

const confirmOptTitle = () => {
  if (selectedOptTitle.value && exportArticle.value) {
    exportArticle.value.title = selectedOptTitle.value
    message.success('标题已替换')
  }
  titleOptVisible.value = false
}

const closeTitleOpt = () => {
  titleOptVisible.value = false
}

const cardStyles = {
  xiaohongshu: {
    label: '小红书',
    accent: '#ff2442',
    coverGrad: ['#ff2442', '#ff7a8a', '#ffd1d9'],
    coverCircle: 'rgba(255,255,255,0.18)',
    coverCircle2: 'rgba(255,255,255,0.12)',
    tagBg: 'rgba(255,255,255,0.95)',
    tagText: '干货分享',
    brandText: '— 作者昵称 —',
    contentBg: '#fff',
    headingColor: '#1a1a1a',
    bodyColor: '#595959',
    numColor: '#fff',
    footerBg: '#fff0f3',
    font: '"PingFang SC", sans-serif'
  },
  wechat: {
    label: '公众号',
    accent: '#07c160',
    coverGrad: ['#07c160', '#95de64', '#d9f7be'],
    coverCircle: 'rgba(255,255,255,0.2)',
    coverCircle2: 'rgba(255,255,255,0.14)',
    tagBg: 'rgba(255,255,255,0.95)',
    tagText: '深度好文',
    brandText: '— 作者昵称 —',
    contentBg: '#fff',
    headingColor: '#1a1a1a',
    bodyColor: '#595959',
    numColor: '#fff',
    footerBg: '#f6ffed',
    font: '"PingFang SC", sans-serif'
  },
  douyin: {
    label: '抖音',
    accent: '#25f4ee',
    coverGrad: ['#0a0a0a', '#1a1a1a', '#fe2c55'],
    coverCircle: 'rgba(37,244,238,0.25)',
    coverCircle2: 'rgba(254,44,85,0.25)',
    tagBg: '#25f4ee',
    tagText: '上热门',
    brandText: '— 作者昵称 —',
    contentBg: '#1a1a1a',
    headingColor: '#fff',
    bodyColor: '#d9d9d9',
    numColor: '#0a0a0a',
    footerBg: '#000',
    font: '"PingFang SC", sans-serif'
  },
  literary: {
    label: '文艺',
    accent: '#8b5e34',
    coverGrad: ['#8b5e34', '#d4a373', '#f0e6d8'],
    coverCircle: 'rgba(255,255,255,0.18)',
    coverCircle2: 'rgba(255,255,255,0.12)',
    tagBg: 'rgba(255,255,255,0.92)',
    tagText: '慢读时光',
    brandText: '— 作者昵称 —',
    contentBg: '#faf5ef',
    headingColor: '#5a3e2b',
    bodyColor: '#8b5e34',
    numColor: '#fff',
    footerBg: '#f0e6d8',
    font: 'Georgia, "Songti SC", serif'
  },
  minimal: {
    label: '极简',
    accent: '#1a1a1a',
    coverGrad: ['#1a1a1a', '#262626', '#404040'],
    coverCircle: 'rgba(255,255,255,0.06)',
    coverCircle2: 'rgba(255,255,255,0.04)',
    tagBg: '#fff',
    tagText: 'NOTE',
    brandText: '— YOUR NAME —',
    contentBg: '#fff',
    headingColor: '#000',
    bodyColor: '#262626',
    numColor: '#fff',
    footerBg: '#fafafa',
    font: '"Helvetica Neue", "PingFang SC", sans-serif'
  },
  business: {
    label: '商务',
    accent: '#1677ff',
    coverGrad: ['#003a8c', '#1677ff', '#bae0ff'],
    coverCircle: 'rgba(255,255,255,0.15)',
    coverCircle2: 'rgba(255,255,255,0.1)',
    tagBg: 'rgba(255,255,255,0.95)',
    tagText: 'INSIGHT',
    brandText: '— YOUR NAME —',
    contentBg: '#fff',
    headingColor: '#003a8c',
    bodyColor: '#595959',
    numColor: '#fff',
    footerBg: '#f0f5ff',
    font: '"PingFang SC", sans-serif'
  }
}

const cardStyleKeys = computed(() => Object.keys(cardStyles))

const generateExportCards = () => {
  if (!exportArticle.value) return

  const title = exportArticle.value.title || '未命名文章'
  const body = exportArticle.value.body || ''
  const paragraphs = body.split(/\n+/).filter(line => line.trim())

  const cards = []
  const firstP = paragraphs.find(p => !p.startsWith('【'))
  cards.push({
    type: 'cover',
    title,
    desc: firstP ? firstP.trim().slice(0, 80) : ''
  })

  let currentHeading = null
  let currentContent = []
  let headingIndex = 0

  paragraphs.forEach(line => {
    const headingMatch = line.match(/^【([^】]+)】$/)
    if (headingMatch) {
      if (currentHeading) {
        cards.push({
          type: 'content',
          num: headingIndex,
          title: currentHeading,
          content: currentContent.slice(0, 5).join('\n')
        })
      }
      headingIndex++
      currentHeading = headingMatch[1].trim()
      currentContent = []
    } else if (currentHeading) {
      currentContent.push(line.trim().slice(0, 120))
    }
  })

  if (currentHeading) {
    cards.push({
      type: 'content',
      num: headingIndex,
      title: currentHeading,
      content: currentContent.slice(0, 5).join('\n')
    })
  }

  cardsData.value = cards
  cardsStyle.value = 'xiaohongshu'
  cardsModalVisible.value = true
}

const closeExportCardsModal = () => {
  cardsModalVisible.value = false
}

const wrapCardText = (ctx, text, x, y, maxWidth, lineHeight, maxLines) => {
  if (!text) return y
  const chars = text.split('')
  let line = ''
  let yy = y
  let drawnLines = 0

  for (let i = 0; i < chars.length; i++) {
    line += chars[i]
    if (ctx.measureText(line).width > maxWidth) {
      line = line.slice(0, -1)
      if (drawnLines >= maxLines - 1 && i < chars.length - 1) {
        while (ctx.measureText(line + '...').width > maxWidth) line = line.slice(0, -1)
        ctx.fillText(line + '...', x, yy)
        return yy + lineHeight
      }
      ctx.fillText(line, x, yy)
      drawnLines++
      yy += lineHeight
      line = chars[i]
    }
  }

  if (line) {
    if (drawnLines >= maxLines - 1) {
      while (ctx.measureText(line + '...').width > maxWidth) line = line.slice(0, -1)
      ctx.fillText(line + '...', x, yy)
    } else {
      ctx.fillText(line, x, yy)
    }
  }
  return yy
}

const drawExportCard = (canvas, data, styleName) => {
  const style = cardStyles[styleName] || cardStyles.xiaohongshu
  const ctx = canvas.getContext('2d')
  const w = canvas.width
  const h = canvas.height
  ctx.clearRect(0, 0, w, h)

  if (data.type === 'cover') {
    const grad = ctx.createLinearGradient(0, 0, w, h)
    grad.addColorStop(0, style.coverGrad[0])
    grad.addColorStop(0.5, style.coverGrad[1])
    grad.addColorStop(1, style.coverGrad[2])
    ctx.fillStyle = grad
    ctx.fillRect(0, 0, w, h)

    ctx.fillStyle = style.coverCircle || 'rgba(255,255,255,0.18)'
    ctx.beginPath(); ctx.arc(w - 80, 120, 160, 0, Math.PI * 2); ctx.fill()
    ctx.beginPath(); ctx.arc(60, h - 220, 120, 0, Math.PI * 2); ctx.fill()
    ctx.fillStyle = style.coverCircle2 || 'rgba(255,255,255,0.12)'
    ctx.beginPath(); ctx.arc(w - 200, h - 120, 90, 0, Math.PI * 2); ctx.fill()

    ctx.fillStyle = style.tagBg
    ctx.beginPath()
    ctx.moveTo(60 + 80, 80)
    ctx.arcTo(60 + 160, 80, 60 + 160, 80 + 44, 22)
    ctx.arcTo(60 + 160, 80 + 44, 60, 80 + 44, 22)
    ctx.arcTo(60, 80 + 44, 60, 80, 22)
    ctx.arcTo(60, 80, 60 + 160, 80, 22)
    ctx.closePath()
    ctx.fill()
    ctx.fillStyle = style.accent
    ctx.font = `bold 22px ${style.font}`
    ctx.textAlign = 'center'
    ctx.fillText(style.tagText, 140, 111)

    ctx.fillStyle = '#fff'
    ctx.font = `bold 60px ${style.font}`
    ctx.textAlign = 'left'
    ctx.shadowColor = 'rgba(0,0,0,0.1)'
    ctx.shadowBlur = 8
    ctx.shadowOffsetY = 4
    wrapCardText(ctx, data.title, 60, 320, w - 120, 78, 4)
    ctx.shadowColor = 'transparent'
    ctx.shadowBlur = 0

    ctx.fillStyle = 'rgba(255,255,255,0.92)'
    ctx.font = `26px ${style.font}`
    wrapCardText(ctx, data.desc, 60, h - 220, w - 120, 40, 2)

    ctx.fillStyle = '#fff'
    ctx.font = `bold 26px ${style.font}`
    ctx.fillText(style.brandText, 60, h - 80)
  } else {
    const bg = style.contentBg || '#fff'
    ctx.fillStyle = bg
    ctx.fillRect(0, 0, w, h)

    ctx.fillStyle = style.accent
    ctx.fillRect(0, 0, w, 14)

    ctx.fillStyle = style.accent
    ctx.beginPath()
    ctx.arc(110, 140, 56, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = style.numColor || '#fff'
    ctx.font = `bold 48px ${style.font}`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const numStr = String(data.num).padStart(2, '0')
    ctx.fillText(numStr, 110, 148)
    ctx.textBaseline = 'alphabetic'

    ctx.fillStyle = style.headingColor
    ctx.font = `bold 46px ${style.font}`
    ctx.textAlign = 'left'
    const titleEndY = wrapCardText(ctx, data.title, 60, 260, w - 120, 60, 2)

    ctx.fillStyle = style.accent
    ctx.fillRect(60, titleEndY + 8, 80, 5)

    ctx.fillStyle = style.bodyColor
    ctx.font = `28px ${style.font}`
    let contentY = titleEndY + 60
    const lines = (data.content || '').split('\n')
    lines.forEach(line => {
      if (!line.trim()) return
      wrapCardText(ctx, line.trim(), 60, contentY, w - 120, 44, 1)
      contentY += 50
    })

    ctx.fillStyle = style.footerBg
    ctx.fillRect(0, h - 110, w, 110)
    ctx.fillStyle = style.accent
    ctx.font = `bold 22px ${style.font}`
    ctx.fillText(style.brandText, 60, h - 55)
  }
}

const renderExportCardCanvas = (index) => {
  const canvas = cardCanvasRefs.value[index]
  if (!canvas) return null
  const card = cardsData.value[index]
  drawExportCard(canvas, card, cardsStyle.value)
  return canvas
}

const downloadExportCanvas = (canvas, filename) => {
  canvas.toBlob((blob) => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }, 'image/png')
}

const downloadExportCard = (index) => {
  const canvas = renderExportCardCanvas(index)
  if (!canvas) return
  const style = cardStyles[cardsStyle.value]
  downloadExportCanvas(canvas, `${style.label}_贴图_${index + 1}_${cardsData.value.length}.png`)
}

const downloadAllExportCards = () => {
  cardsData.value.forEach((_, index) => {
    setTimeout(() => {
      downloadExportCard(index)
    }, index * 400)
  })
}

watch([cardsModalVisible, cardsStyle], () => {
  if (!cardsModalVisible.value) return
  nextTick(() => {
    cardCanvasRefs.value.forEach((canvas, index) => {
      if (canvas) drawExportCard(canvas, cardsData.value[index], cardsStyle.value)
    })
  })
}, { immediate: true })

// 操作
const handleSaveDraft = () => {
  const data = {
    id: Date.now(),
    customTitle: customTitle.value,
    customRequirement: customRequirement.value,
    platform: currentPlatform.value,
    wordCount: currentWordCount.value,
    style: currentStyle.value,
    template: currentTemplate.value,
    savedAt: new Date().toISOString()
  }
  console.log('saving draft:', data)
  const drafts = JSON.parse(localStorage.getItem('aichuangzuo_drafts') || '[]')
  drafts.unshift(data)
  localStorage.setItem('aichuangzuo_drafts', JSON.stringify(drafts))
  draftBoxKey.value++
  message.success('草稿已保存')
}

const handlePreview = () => {
  router.push('/console/preview')
}

const handleGenerate = () => {
  if (!customTitle.value.trim()) {
    message.warning('请输入文章标题')
    return
  }
  if (!customRequirement.value.trim()) {
    message.warning('请补充你的核心观点和要求')
    return
  }

  // 构建任务数据
  const taskData = {
    title: customTitle.value,
    platform: currentPlatform.value,
    wordCount: currentWordCount.value,
    style: currentStyle.value,
    template: currentTemplate.value,
    customTitle: customTitle.value,
    customRequirement: customRequirement.value
  }

  // 直接添加到本地队列并开始模拟生成
  addToMiniQueue(taskData)

  // 清空表单
  clearForm()
  message.success('已加入生成队列')
}

// 生成模拟文章内容
const generateMockContent = (item) => {
  const title = item.title
  return {
    title,
    body: `【引言】\n\n在快节奏的当下，${title}成为许多人关注的焦点。无论是职场新人还是资深从业者，掌握正确的方法都能带来显著的效率提升。\n\n【方法一：明确目标，拆解任务】\n\n首先要做的是把大目标拆成可执行的小任务。模糊的目标容易让人拖延，而清晰的步骤能帮助你快速启动。建议每天早晨列出当天最重要的三件事，并预估完成时间。\n\n【方法二：建立节奏，减少切换】\n\n频繁切换任务会大量消耗注意力。尝试用番茄工作法或时间块管理，把同类工作集中处理，减少上下文切换带来的损耗。\n\n【方法三：复盘迭代，持续优化】\n\n每完成一个阶段，花五分钟回顾：哪些做得好？哪些可以改进？长期积累下来，你会形成一套适合自己的高效 workflow。\n\n【结语】\n\n${title}并不难，关键在于从今天开始行动。选择适合自己的方法，坚持下去，效果会逐渐显现。`,
    style: item.style || '专业严谨'
  }
}

// 添加到本地队列并模拟生成
const addToMiniQueue = (taskData) => {
  const item = {
    id: Date.now(),
    title: taskData.title,
    platform: taskData.platform?.name || '未选择',
    wordCount: taskData.wordCount?.count || 0,
    style: taskData.style?.name || '未选择',
    template: taskData.template?.name || '未选择',
    status: 'generating',
    progress: 0,
    createdAt: new Date().toISOString(),
    completedAt: null
  }
  miniQueueList.value.unshift(item)
  saveMiniQueue()

  // 模拟生成进度
  const interval = setInterval(() => {
    const current = miniQueueList.value.find(x => x.id === item.id)
    if (!current || current.status !== 'generating') {
      clearInterval(interval)
      return
    }
    current.progress = Math.min(100, Math.round((current.progress + Math.random() * 15 + 5) * 10) / 10)
    if (current.progress >= 100) {
      current.progress = 100
      current.status = 'completed'
      current.completedAt = new Date().toISOString()
      current.content = generateMockContent(current)
      saveMiniQueue()
      clearInterval(interval)
    } else {
      saveMiniQueue()
    }
  }, 500)
}

const saveMiniQueue = () => {
  localStorage.setItem('aichuangzuo_generation_queue', JSON.stringify(miniQueueList.value))
}

const clearForm = () => {
  customTitle.value = ''
  customRequirement.value = ''
}
</script>

<style scoped>
.create-index {
  display: flex;
  flex-direction: column;
}

.create-layout {
  display: flex;
  gap: 24px;
}

.create-card {
  background: var(--color-bg-card);
  border-radius: 16px;
  padding: 20px;
  flex: 1;
  min-width: 0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

/* 生成队列面板 */
.queue-panel {
  width: 320px;
  flex-shrink: 0;
  background: #fafafa;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
}

.queue-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.queue-panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.queue-more-btn {
  background: none;
  border: none;
  color: #8c8c8c;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.queue-more-btn:hover {
  color: var(--color-primary);
  background: #fff0f2;
}

.queue-panel-more {
  margin-top: 12px;
  padding: 10px 12px;
  background: #fff;
  border: 1px dashed #ffd1d9;
  border-radius: 10px;
  font-size: 13px;
  color: #595959;
  text-align: center;
  line-height: 1.6;
}

.queue-panel-more-link {
  display: inline;
  padding: 0;
  border: none;
  background: transparent;
  color: #ff2442;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
}

.queue-panel-more-link:hover {
  color: #e61e3a;
  text-decoration: underline;
}

.queue-panel-empty {
  text-align: center;
  padding: 24px 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.queue-panel-empty .empty-icon {
  font-size: 32px;
  color: #d9d9d9;
  margin-bottom: 8px;
}

.queue-panel-empty .empty-text {
  color: #8c8c8c;
  font-size: 14px;
  margin-bottom: 4px;
}

.queue-panel-empty .empty-hint {
  color: #bfbfbf;
  font-size: 12px;
}

.queue-panel-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-x: hidden;
  flex: 1;
  padding-right: 4px;
}

.queue-panel-item {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 14px;
  transition: all 0.2s;
}

.queue-panel-item:hover {
  border-color: #ffd1d9;
  box-shadow: 0 2px 12px rgba(255, 36, 66, 0.08);
}

.queue-panel-item.generating {
  background: #fff;
  border-color: #ffd1d9;
}

.queue-panel-item.completed {
  background: #fff;
  border-color: #d9f7be;
}

.queue-panel-item.completed:hover {
  border-color: #b7eb8f;
  box-shadow: 0 2px 8px rgba(7, 193, 96, 0.08);
}

.queue-panel-item.queued {
  background: #fff;
  border-color: #e8e8e8;
}

.queue-panel-item.failed {
  background: #fff;
  border-color: #ffccc7;
}

.queue-item-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.queue-item-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.queue-panel-item.generating .queue-item-icon {
  background: #fff0f2;
  color: #ff2442;
}

.queue-panel-item.completed .queue-item-icon {
  background: #f0fff2;
  color: #07c160;
}

.queue-panel-item.queued .queue-item-icon {
  background: #f5f5f5;
  color: #8c8c8c;
}

.queue-panel-item.failed .queue-item-icon {
  background: #fff0f0;
  color: #ff4d4f;
}

.queue-item-info {
  flex: 1;
  min-width: 0;
}

.queue-item-title {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
}

.queue-item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.queue-item-status-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.queue-item-status-badge.generating {
  background: #ffeaea;
  color: #ff2442;
}

.queue-item-status-badge.completed {
  background: #e6fff2;
  color: #07c160;
}

.queue-item-status-badge.queued {
  background: #f5f5f5;
  color: #595959;
}

.queue-item-status-badge.failed {
  background: #fff0f0;
  color: #ff4d4f;
}

.queue-item-progress-text {
  font-size: 12px;
  font-weight: 600;
  color: #ff2442;
}

.queue-item-progress {
  margin-top: 12px;
}

.queue-item-progress .progress-bar {
  height: 6px;
  background: rgba(255, 36, 66, 0.15);
  border-radius: 3px;
  overflow: hidden;
}

.queue-item-progress .progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff2442, #ff6b81);
  border-radius: 3px;
  transition: width 0.3s;
}

.queue-item-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e8e8e8;
  display: flex;
  justify-content: flex-end;
}

.queue-export-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #fff;
  color: #ff2442;
  border: 1.5px solid #ff2442;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.queue-export-btn:hover {
  background: #fff0f2;
}

.queue-export-btn:active {
  background: #ffeaea;
}

.create-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.create-subtitle {
  color: var(--color-text-secondary);
  font-size: 14px;
  margin-bottom: 20px;
}

/* 额度卡片 */
.quota-card {
  background: #fff0f2;
  border: 1px solid #ffbdc5;
  border-radius: 8px;
  padding: 10px 16px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.quota-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  margin-bottom: 8px;
}

.quota-text {
  color: #595959;
}

.quota-used {
  color: #ff4d4f;
}

.quota-remaining {
  color: #ff4d4f;
  font-weight: 600;
}

.quota-link {
  color: #ff4d4f;
  cursor: pointer;
  font-size: 12px;
}

.quota-link:hover {
  text-decoration: underline;
}

.quota-progress {
  height: 4px;
  background: #fff;
  border-radius: 2px;
  overflow: hidden;
}

.quota-progress-bar {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s, background 0.3s;
}

/* 模式切换 */
.mode-tabs {
  display: flex;
  gap: 8px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 10px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.mode-tab {
  flex: 1;
  padding: 10px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 选题 */
.topic-section {
  display: flex;
  flex-direction: column;
}

.topic-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 8px;
}

.topic-card {
  padding: 8px 10px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.topic-card:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.topic-card.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.topic-card.used {
  opacity: 0.5;
  cursor: not-allowed;
}

.topic-card.used:hover {
  border-color: #f0f0f0;
  background: transparent;
}

.topic-title {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 13px;
  line-height: 1.3;
  margin-bottom: 4px;
}

.topic-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.topic-tag {
  font-size: 11px;
  color: var(--color-primary);
  background: #fff0f2;
  padding: 2px 6px;
  border-radius: 4px;
}

.topic-heat {
  font-size: 11px;
  color: #8c8c8c;
}

.topic-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.topic-hint {
  font-size: 13px;
  color: #8c8c8c;
}

.refresh-btn {
  padding: 6px 12px;
  background: #fff;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover {
  background: var(--color-primary);
  color: #fff;
}

/* 输入区域 */
.input-section {
  margin-bottom: 12px;
}

.create-mode {
  display: flex;
  flex-direction: column;
}

.form-label {
  display: block;
  font-weight: 500;
  color: #262626;
  margin-bottom: 8px;
}

.required {
  color: #ff4d4f;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 15px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.form-input::placeholder {
  color: #bfbfbf;
}

.form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 15px;
  min-height: 100px;
  resize: vertical;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.form-textarea::placeholder {
  color: #bfbfbf;
}

.input-hint {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 6px;
}

/* 设置工具栏 */
.settings-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  font-size: 14px;
  flex-shrink: 0;
  color: #595959;
}

.settings-label {
  color: #8c8c8c;
  font-size: 13px;
}

.settings-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  color: #262626;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.settings-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fff0f2;
}

.chip-caret {
  font-size: 10px;
  color: #8c8c8c;
}

.settings-sep {
  color: #d9d9d9;
  font-size: 12px;
  user-select: none;
}

/* 操作栏 */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.action-bar-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-link {
  display: inline-flex;
  align-items: center;
  background: none;
  border: none;
  color: #595959;
  font-size: 14px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.action-link:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.action-primary {
  display: inline-flex;
  align-items: center;
  padding: 12px 28px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.25);
}

.action-primary:hover {
  background: var(--color-primary-hover);
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.35);
}

/* 平台选择 */
.platform-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 8px 0;
}

.platform-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.platform-item:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.platform-item.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.platform-name {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 15px;
}

.platform-desc {
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.5;
}

.modal-title-wrap {
  padding-right: 28px;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.modal-subtitle {
  font-size: 13px;
  color: #8c8c8c;
}

/* 字数选择 */
.wc-tabs {
  display: flex;
  gap: 6px;
  padding: 0 0 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
  overflow-x: auto;
}

.wc-tab {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.wc-tab.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fff0f2;
}

.wc-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.wc-item {
  padding: 14px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.wc-item:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.wc-item.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.wc-count {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.wc-label {
  font-size: 12px;
  color: #8c8c8c;
}

.wc-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.wc-item-wide {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.wc-item-wide:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.wc-item-wide.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.wc-item-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.wc-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.wc-custom {
  padding: 8px 4px;
}

.wc-custom-display {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-primary);
  text-align: center;
  margin: 16px 0;
}

.wc-custom-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 18px;
  text-align: center;
  box-sizing: border-box;
}

.wc-slider {
  width: 100%;
  margin-top: 16px;
  accent-color: var(--color-primary);
}

.wc-slider::-webkit-slider-thumb {
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--color-primary);
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.wc-slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--color-primary);
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.wc-custom-hint {
  color: #8c8c8c;
  font-size: 12px;
  margin-top: 12px;
  text-align: center;
}

/* 风格选择 */
.style-tabs {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
}

.style-tab {
  padding: 8px 0;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.style-tab.active {
  color: var(--color-primary);
  font-weight: 600;
  border-bottom-color: var(--color-primary);
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.style-content {
  max-height: 60vh;
  overflow-y: auto;
}

.style-card {
  padding: 16px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.style-card:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.style-card.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.style-card-title {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 15px;
  margin-bottom: 4px;
}

.style-card-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.style-card-scope {
  font-size: 12px;
  color: #1890ff;
  background: #e6f7ff;
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.style-card-prompt {
  font-size: 12px;
  color: #595959;
  line-height: 1.6;
  white-space: pre-line;
}

.style-card-count {
  font-size: 12px;
  color: var(--color-primary);
}

.style-add-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  border: 2px dashed #d9d9d9;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 100px;
}

.style-add-card:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.style-add-icon {
  font-size: 24px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.style-add-text {
  font-size: 13px;
  color: #8c8c8c;
}

.style-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 32px 0;
}

.style-empty-text {
  color: #8c8c8c;
  font-size: 14px;
}

.style-footer {
  padding: 12px 0 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.style-apply-btn {
  padding: 8px 24px;
  border-radius: 8px;
  border: none;
  background: #d9d9d9;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: not-allowed;
}

.style-apply-btn:not(:disabled) {
  background: var(--color-primary);
  cursor: pointer;
}

.style-apply-btn:not(:disabled):hover {
  background: var(--color-primary-hover);
}

.style-prompt-toggle {
  font-size: 12px;
  color: var(--color-primary);
  cursor: pointer;
  margin-top: 8px;
}

.style-prompt-full {
  font-size: 12px;
  color: #595959;
  line-height: 1.6;
  margin-top: 8px;
  padding: 8px;
  background: #fafafa;
  border-radius: 6px;
  white-space: pre-wrap;
}

.style-prompt-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.style-action-btn {
  font-size: 12px;
  color: var(--color-primary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}

.style-action-btn:hover {
  text-decoration: underline;
}

.style-del-btn {
  color: #ff4d4f;
}

.style-editor-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.style-editor-back {
  background: none;
  border: none;
  color: var(--color-primary);
  cursor: pointer;
  font-size: 14px;
}

.style-editor-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.style-editor-form {
  padding: 0;
}

.style-editor-field {
  margin-bottom: 16px;
}

.style-editor-label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
  font-weight: 500;
}

.style-editor-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  box-sizing: border-box;
}

.style-editor-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.style-editor-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
}

.style-editor-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

.style-editor-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 6px;
}

.style-editor-presets {
  margin-bottom: 16px;
}

.style-editor-preset-label {
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
}

.style-editor-preset-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.style-preset-card {
  padding: 8px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.style-preset-card:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.style-preset-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.style-preset-desc {
  font-size: 11px;
  color: #8c8c8c;
}

.save-style-btn {
  width: 100%;
  padding: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.save-style-btn:hover {
  background: var(--color-primary-hover);
}

/* 模板选择 */
:deep(.template-lib-modal .ant-modal-content) {
  width: 960px;
}

.template-tabs {
  display: flex;
  gap: 8px;
  padding: 0 0 14px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
  overflow-x: auto;
}

.template-tab {
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #595959;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.template-tab.active {
  border-color: #ff2442;
  background: #fff0f2;
  color: #ff2442;
  font-weight: 600;
}

.template-body {
  display: flex;
  gap: 16px;
}

.template-preview-pane {
  flex: 0 0 420px;
  background: #f5f5f5;
  border-radius: 10px;
  overflow: hidden;
  height: 480px;
  box-shadow: inset 0 0 0 1px rgba(0,0,0,0.05);
}

.template-list-pane {
  flex: 1;
  min-width: 0;
  height: 480px;
  overflow-y: auto;
  padding-right: 4px;
}

.template-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}

.template-row:hover {
  border-color: #ff2442;
  background: #fff0f2;
}

.template-row.selected {
  border-color: #ff2442;
  background: #fff0f2;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.25);
}

.template-row-name {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
  margin-bottom: 2px;
}

.template-row-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.template-footer {
  padding: 12px 0 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.template-apply-btn {
  padding: 8px 24px;
  border-radius: 8px;
  border: none;
  background: #ff2442;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.template-apply-btn:hover {
  background: #e61e3a;
}

/* 草稿箱弹框 */
.draft-box-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
}

.draft-empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.draft-empty-text {
  color: #8c8c8c;
  font-size: 14px;
}

.draft-box-list {
  max-height: 400px;
  overflow-y: auto;
}

.draft-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}

.draft-item:hover {
  border-color: #ff2442;
  background: #fff0f2;
}

.draft-title {
  font-weight: 600;
  font-size: 14px;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.draft-desc {
  font-size: 12px;
  color: #595959;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 320px;
}

.draft-meta {
  font-size: 12px;
  color: #8c8c8c;
}

.draft-del-btn {
  padding: 4px 8px;
  border: none;
  background: none;
  color: #8c8c8c;
  font-size: 12px;
  cursor: pointer;
}

.draft-del-btn:hover {
  color: #ff4d4f;
}

/* 导出弹框 */
.modal-title-text {
  font-size: 18px;
  font-weight: 600;
}

.export-modal-content {
  max-height: 70vh;
  overflow-y: auto;
  padding-bottom: 80px;
}

.export-article {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 24px;
}

.export-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 12px;
  line-height: 1.4;
}

.export-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
  flex-wrap: wrap;
}

.export-style-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.export-body {
  font-size: 15px;
  line-height: 1.8;
  color: #262626;
}

.export-body h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 24px 0 12px;
}

.export-publish-meta {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
  position: relative;
}

.export-meta-title {
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  font-size: 15px;
}

.export-desc-input {
  width: 100%;
  min-height: 80px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #262626;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
}

.export-desc-input:focus {
  outline: none;
  border-color: #ff2442;
}

.export-meta-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.export-meta-btn {
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.export-meta-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.export-meta-btn.primary {
  background: #fff;
  border-color: #ff2442;
  color: #ff2442;
}

.export-meta-btn.primary:hover {
  background: #fff0f2;
}

.export-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.export-tag {
  padding: 4px 12px;
  background: #f5f5f5;
  border-radius: 14px;
  font-size: 13px;
  color: #595959;
}

.export-floating-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #eee;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
  padding: 10px 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
}

.export-float-btn {
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.15s;
}

.export-float-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.export-float-btn.primary {
  background: #ff2442;
  border-color: #ff2442;
  color: #fff;
}

.export-float-btn.primary:hover {
  background: #e61e3a;
}

.export-float-btn.outline {
  background: #fff;
  border-color: #ff2442;
  color: #ff2442;
}

.export-float-btn.outline:hover {
  background: #fff0f2;
}

.export-float-btn.danger {
  background: #fff;
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.export-float-btn.danger:hover {
  background: #fff2f0;
}

/* ===== AI 标题优化弹框 ===== */

.title-opt-content {
  padding: 8px 4px 0;
}

.title-opt-original {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 12px 14px;
  font-size: 14px;
  color: #1a1a1a;
  line-height: 1.5;
  margin-bottom: 20px;
}

.title-opt-original span {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.title-opt-section-label {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 10px;
}

.title-opt-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.title-opt-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
}

.title-opt-item:hover {
  border-color: #ff2442;
  background: #fff8f9;
}

.title-opt-item.selected {
  border-color: #ff2442;
  background: #fff0f2;
}

.title-opt-radio {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid #d9d9d9;
  flex-shrink: 0;
  margin-top: 2px;
  transition: all 0.15s;
}

.title-opt-item.selected .title-opt-radio {
  border-color: #ff2442;
  background: #ff2442;
  box-shadow: inset 0 0 0 3px #fff;
}

.title-opt-item-text {
  font-size: 14px;
  color: #1a1a1a;
  line-height: 1.5;
}

.title-opt-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.title-opt-btn-cancel {
  padding: 8px 18px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.15s;
}

.title-opt-btn-cancel:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.title-opt-btn-confirm {
  padding: 8px 18px;
  background: #ff2442;
  border: 1px solid #ff2442;
  border-radius: 6px;
  font-size: 13px;
  color: #fff;
  cursor: pointer;
  transition: all 0.15s;
}

.title-opt-btn-confirm:hover {
  background: #e61e3a;
}

.title-opt-btn-confirm:disabled {
  background: #ffd1d9;
  border-color: #ffd1d9;
  cursor: not-allowed;
}

/* ===== 贴图生成弹框 ===== */

.cards-modal-content {
  padding: 8px 4px 0;
}

.cards-modal-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
  flex-wrap: wrap;
}

.cards-modal-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.cards-modal-tab {
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 20px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.15s;
}

.cards-modal-tab:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.cards-modal-download-all {
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #ff2442;
  border-radius: 20px;
  font-size: 13px;
  color: #ff2442;
  cursor: pointer;
  transition: all 0.15s;
}

.cards-modal-download-all:hover {
  background: #fff0f2;
}

.cards-modal-sub {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 16px;
}

.cards-modal-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  max-height: 520px;
  overflow-y: auto;
  padding-right: 4px;
}

.cards-modal-item {
  cursor: pointer;
  transition: transform 0.15s;
}

.cards-modal-item:hover {
  transform: translateY(-2px);
}

.cards-modal-canvas {
  width: 100%;
  height: auto;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  display: block;
}

.cards-modal-item-label {
  text-align: center;
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .cards-modal-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== 单屏启动器新样式 ===== */

.create-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.create-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}

.create-subtitle {
  font-size: 14px;
  margin-bottom: 0;
}

.quota-pill {
  font-size: 12px;
  color: #595959;
  background: #f5f5f5;
  padding: 4px 10px;
  border-radius: 12px;
  white-space: nowrap;
}

.quota-pill strong {
  color: var(--color-primary);
}

.hero-input {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.hero-title-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #c4c4c4;
  border-radius: 10px;
  font-size: 15px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.hero-title-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.hero-title-input::placeholder {
  color: #999;
}

.hero-textarea {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #c4c4c4;
  border-radius: 12px;
  font-size: 15px;
  min-height: 120px;
  resize: vertical;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
  font-family: inherit;
  line-height: 1.6;
}

.hero-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.hero-textarea::placeholder {
  color: #999;
}

.hero-generate-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: auto;
  padding: 12px 36px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(255, 36, 66, 0.3);
  flex-shrink: 0;
}

.hero-generate-btn:hover {
  background: var(--color-primary-hover);
  box-shadow: 0 6px 20px rgba(255, 36, 66, 0.4);
  transform: translateY(-1px);
}

.hero-generate-btn:active {
  transform: translateY(0);
}

.hero-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.hero-action-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.smart-defaults {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.topic-capsules {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  flex-shrink: 0;
  min-width: 0;
}

.topic-capsules-label {
  font-size: 13px;
  color: #595959;
  flex-shrink: 0;
  line-height: 20px;
}

.topic-capsules-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.topic-capsules-grid > * {
  display: flex;
  min-width: 0;
}

.topic-capsule {
  width: 100%;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  font-size: 12px;
  color: #595959;
  cursor: pointer;
  transition: all 0.15s;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  box-sizing: border-box;
  text-align: left;
}

.topic-capsule:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fff0f2;
}

.topic-capsule.used {
  opacity: 0.5;
  cursor: not-allowed;
}

.refresh-capsule {
  align-self: flex-start;
  padding: 6px 16px;
  background: none;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  font-size: 12px;
  color: #8c8c8c;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  margin-top: 4px;
}

.refresh-capsule:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.create-card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .create-layout {
    flex-direction: column;
  }

  .queue-panel {
    width: 100%;
    box-sizing: border-box;
  }

  .queue-panel-empty {
    padding: 16px 0;
  }

  .create-card {
    padding: 16px;
  }

  .create-card-header {
    flex-direction: column;
    gap: 8px;
  }

  .create-title {
    font-size: 20px;
  }

  .hero-textarea {
    min-height: 100px;
  }

  .smart-defaults {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
    scrollbar-width: none;
  }

  .smart-defaults::-webkit-scrollbar {
    display: none;
  }

  .hero-action-row {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .hero-action-left {
    justify-content: flex-start;
  }

  .hero-generate-btn {
    width: 100%;
  }

  .topic-capsules-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .refresh-capsule {
    align-self: center;
  }
}
</style>