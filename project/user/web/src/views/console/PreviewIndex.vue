<template>
  <div class="preview-index">
    <div class="preview-header">
      <button class="back-btn" @click="$router.push('/console/works')">
        ← 返回
      </button>
      <h2 class="preview-title-text">预览/导出</h2>
      <div class="preview-header-actions">
        <button class="action-btn" @click="goToEditPage">
          编辑正文
        </button>
        <button class="action-btn" @click="copyText">
          <CopyOutlined /> 复制正文
        </button>
        <button class="action-btn primary" @click="exportWord">
          导出 Word
        </button>
      </div>
    </div>

    <div v-if="!article" class="preview-empty">
      <div class="empty-icon">📄</div>
      <div class="empty-text">暂无文章内容</div>
      <button class="empty-btn" @click="$router.push('/console/create')">去创作</button>
    </div>

    <div v-else class="preview-content">
      <div class="preview-article" :style="{ background: templateStyle.bg, fontFamily: templateStyle.font }">
        <h1 class="article-title" :style="{ color: templateStyle.titleColor, fontSize: templateStyle.titleSize, textAlign: templateStyle.titleAlign || 'left' }">{{ article.title }}</h1>
        <div class="article-meta" :style="{ color: templateStyle.metaColor, borderBottomColor: templateStyle.metaBorder, textAlign: templateStyle.metaAlign || 'left' }">
          <span>{{ formatDate(article.completedAt) }}</span>
          <span>·</span>
          <span>约 {{ article.wordCount }} 字</span>
          <span class="article-style-badge">
            风格:{{ article.style || '专业严谨' }}
          </span>
          <span v-if="templateMeta" class="article-template-badge">
            模板:{{ templateMeta.name }}
          </span>
        </div>
        <div v-if="!isEditing" class="article-body" v-html="formattedBody" @click="onBodyClick"></div>
        <div v-else class="article-body editing-body" @click="onBodyClick">
          <div
            v-for="(block, idx) in blocks"
            :key="idx"
            :class="['edit-block', block.type, { modified: modifiedIndices.has(idx) }]"
            :data-type="block.type"
            contenteditable="true"
            @paste="onPaste"
            @input="onBlockInput(idx, $event)"
            v-html="renderBlockHtml(block)"
          />
        </div>
      </div>

      <!-- 发布描述 -->
      <div class="publish-meta-card">
        <div class="meta-section-title">发布描述</div>
        <textarea
          v-model="publishDesc"
          class="publish-desc-input"
          placeholder="输入发布描述..."
        ></textarea>
        <div class="meta-actions">
          <button class="meta-btn primary" @click="copyDesc">复制描述</button>
        </div>

        <div class="meta-section-title" style="margin-top: 24px;">
          推荐标签
          <a-popover
            placement="topLeft"
            :open="tagHelpOpen"
            overlay-class-name="tag-help-popover"
          >
            <template #content>
              <div
                class="tag-help-panel"
                @mouseenter="openTagHelp"
                @mouseleave="scheduleCloseTagHelp"
              >
                <div class="tag-help-tabs">
                  <span
                    v-for="p in tagPlatforms"
                    :key="p.key"
                    :class="['tag-help-tab', { active: p.key === activeTagPlatform }]"
                    @click="selectTagPlatform(p.key)"
                  >{{ p.name }}</span>
                </div>
                <ol class="tag-help-steps">
                  <li v-for="(s, i) in currentTagTip.steps" :key="i">{{ s }}</li>
                </ol>
                <div
                  v-if="currentTagTip.image"
                  class="tag-help-img-wrap"
                  @click="openTagImgPreview"
                >
                  <img
                    class="tag-help-img"
                    :src="currentTagTip.image"
                    :alt="`${currentTagTip.name}标签设置示意`"
                  />
                  <span class="tag-help-img-hint">点击放大查看</span>
                </div>
                <div class="tag-help-footer">标签贵精不贵多，挑 3~5 个和内容最匹配的即可。</div>
              </div>
            </template>
            <QuestionCircleOutlined
              class="tag-help-icon"
              @mouseenter="openTagHelp"
              @mouseleave="scheduleCloseTagHelp"
              @click.stop="toggleTagHelp"
            />
          </a-popover>
        </div>
        <div class="publish-tags">
          <span
            v-for="tag in publishTags"
            :key="tag"
            class="publish-tag"
          >
            {{ tag }}
          </span>
        </div>
        <div class="meta-actions">
          <button class="meta-btn primary" @click="copyTags">复制全部标签</button>
        </div>
      </div>
    </div>

    <!-- 浮动操作栏 -->
    <div v-if="article && !isEditing" class="floating-action-bar">
      <button class="float-btn" @click="optimizeTitle">
        ✧ AI 优化标题
      </button>
      <button class="float-btn primary" @click="exportWord">
        导出 Word
      </button>
      <button class="float-btn outline" @click="copyText">
        复制正文
      </button>
      <button class="float-btn danger" @click="generateCards">
        生成贴图
      </button>
    </div>

    <!-- 编辑态保存条 -->
    <div v-if="article && isEditing" class="edit-floating-bar">
      <span class="edit-hint">{{ modifiedIndices.size > 0 ? `已修改 ${modifiedIndices.size} 处` : '正在编辑' }}</span>
      <button class="float-btn" @click="cancelEdit">取消</button>
      <button class="float-btn primary" @click="saveEdit">保存修改</button>
    </div>

    <!-- AI 标题优化弹窗 -->
    <Teleport to="body">
      <Transition name="title-opt-fade">
        <div v-if="titleOptVisible" class="title-opt-overlay" @click.self="closeTitleOpt">
          <div class="title-opt-box">
            <button class="title-opt-close" @click="closeTitleOpt">&times;</button>
            <div class="title-opt-header">AI 标题优化</div>

            <div class="title-opt-section">
              <div class="title-opt-original">
                <span>原标题</span>{{ article.title }}
              </div>
              <div class="title-opt-section-label">
                <span>平台适配标题</span>
              </div>
              <div class="title-opt-platform-tabs">
                <button
                  v-for="tab in platformTabMeta"
                  :key="tab.key"
                  class="title-opt-platform-tab"
                  :class="{ active: currentPlatform === tab.key }"
                  @click="currentPlatform = tab.key"
                >
                  {{ tab.label }}
                </button>
              </div>
              <div v-if="titleOptLoading" class="title-opt-loading">AI 生成中，请稍候…</div>
              <div v-else class="title-opt-list">
                <div
                  v-for="(title, index) in currentPlatformTitles"
                  :key="`platform-${index}`"
                  class="title-opt-item"
                  :class="{ selected: selectedTitle === title }"
                  @click="selectTitle(title)"
                >
                  <div class="title-opt-radio"></div>
                  <div class="title-opt-item-text">{{ title }}</div>
                </div>
              </div>
            </div>

            <div class="title-opt-footer">
              <button class="title-opt-btn-cancel" @click="closeTitleOpt">取消</button>
              <button class="title-opt-btn-confirm" :disabled="!selectedTitle" @click="confirmTitle">确认替换</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <CardsModal v-model:visible="cardsModalVisible" :article="article" />

    <!-- 标签示意图放大查看 -->
    <Teleport to="body">
      <Transition name="tag-img-fade">
        <div v-if="tagImgPreviewVisible" class="tag-img-lightbox" @click="tagImgPreviewVisible = false">
          <img
            class="tag-img-lightbox-img"
            :src="currentTagTip.image"
            :alt="`${currentTagTip.name}标签设置示意`"
          />
          <div class="tag-img-lightbox-tip">点击任意位置关闭</div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'

const router = useRouter()
const route = useRoute()
import { CopyOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
import { parseBodyToBlocks, serializeBlocksToArticle, BLOCK_TYPES, stripLeadingTitle, applyStyleOverrides } from '@/utils/articleBlocks.js'
import { useExportTemplates, DEFAULT_TEMPLATE_STYLE } from '@/composables/useExportTemplates.js'
import { getArticle, updateArticle, optimizeTitles } from '@/api/article.js'
import CardsModal from '@/components/CardsModal.vue'

const article = ref(null)
const publishDesc = ref('')
const publishTags = ref([])

// 推荐标签用法：不同平台打标签的方式不一样，弹框按平台说明+操作示意图，默认选中当前文章平台
const tagPlatforms = [
  {
    key: 'wechat', name: '公众号',
    image: 'https://foruda.gitee.com/images/1784167828824884992/0c5d3dfa_8060302.png',
    steps: [
      '在正文里手动输入 # + 话题（如 #公众号），会变成蓝色可点击话题',
      '话题展示在文章中，读者点击可进入同话题文章列表，带来额外曝光'
    ]
  },
  {
    key: 'xiaohongshu', name: '小红书',
    image: 'https://foruda.gitee.com/images/1784168148721501191/f71a48ae_8060302.png',
    steps: [
      '把标签粘贴到笔记正文末尾，每个标签前加上 #（如 #好物分享）',
      '输入 # 时会弹出话题联想，优先选有热度的官方话题',
      '标签会变成蓝色可点击话题，是笔记曝光的重要入口'
    ]
  },
  {
    key: 'toutiao', name: '今日头条',
    image: 'https://foruda.gitee.com/images/1784167935895916922/47b6fcc5_8060302.png',
    steps: [
      '在正文里输入 #，会弹出「最近使用 / 热门话题」联想面板',
      '选中话题即插入蓝色话题（敲空格可取消），优先挑讨论量高的'
    ]
  },
  {
    key: 'baijiahao', name: '百家号',
    image: 'https://foruda.gitee.com/images/1784167699000127132/cd3016a7_8060302.png',
    steps: [
      '文章编辑页工具栏点「插入」，在下拉菜单里选择「# 话题」',
      '搜索并选中话题后插入文章，话题会随文章一起参与分发'
    ]
  },
  {
    key: 'douyin', name: '抖音图文',
    image: 'https://foruda.gitee.com/images/1784168055202970238/4e05dd2c_8060302.png',
    steps: [
      '创作者中心发布页找到「添加话题」栏，点击后搜索选择话题（最多 5 个）',
      '也可在描述文案末尾带上 #标签，蹭相关热点话题流量更大'
    ]
  },
  {
    key: 'zhihu', name: '知乎',
    image: 'https://foruda.gitee.com/images/1784168246131785868/ea45f1d6_8060302.png',
    steps: [
      '文章发布设置页找到「文章话题」栏，点「+ 添加话题」搜索选择',
      '优先绑定关注人数多的大话题，再补 1~2 个精准小话题'
    ]
  },
  {
    key: 'bilibili', name: 'B站',
    image: 'https://foruda.gitee.com/images/1784168311912082962/a2889f51_8060302.png',
    steps: [
      '创作中心投稿页的发布设置里找到「话题」栏，点「+ 添加话题」搜索选择',
      '话题参与推荐分发，选和内容分区匹配的话题效果更好'
    ]
  }
]
const activeTagPlatform = ref('wechat')
const currentTagTip = computed(() =>
  tagPlatforms.find(p => p.key === activeTagPlatform.value) || tagPlatforms[0]
)
const tagHelpOpen = ref(false)
let tagHelpPinned = false
let tagHelpCloseTimer = null
const openTagHelp = () => {
  clearTimeout(tagHelpCloseTimer)
  tagHelpOpen.value = true
}
const scheduleCloseTagHelp = () => {
  clearTimeout(tagHelpCloseTimer)
  tagHelpCloseTimer = setTimeout(() => {
    if (!tagHelpPinned) tagHelpOpen.value = false
  }, 150)
}
// click 固定/取消固定：hover 已展开时点击 → 固定住（触屏 tap 也走这条路）；
// 已固定时再点 → 关闭
const toggleTagHelp = () => {
  clearTimeout(tagHelpCloseTimer)
  if (tagHelpOpen.value && tagHelpPinned) {
    tagHelpOpen.value = false
    tagHelpPinned = false
  } else {
    tagHelpOpen.value = true
    tagHelpPinned = true
  }
}
// 点 tab 是主动交互 → 固定弹框：有图/无图 tab 切换时面板高度变化、
// 底边锚定导致鼠标落到面板外，不固定的话弹框会在用户点击过程中意外关闭
const pinTagHelp = () => {
  clearTimeout(tagHelpCloseTimer)
  tagHelpPinned = true
  tagHelpOpen.value = true
}
const selectTagPlatform = (key) => {
  activeTagPlatform.value = key
  pinTagHelp()
}
// 示意图放大查看：点开时固定弹框，避免鼠标移向大图时弹框关闭
const tagImgPreviewVisible = ref(false)
const openTagImgPreview = () => {
  pinTagHelp()
  tagImgPreviewVisible.value = true
}
const onDocClickCloseTagHelp = (e) => {
  if (!tagHelpOpen.value) return
  if (e.target.closest('.tag-help-popover') || e.target.closest('.tag-help-icon')) return
  // 放大查看遮罩上的点击只是关闭大图，不关弹框
  if (e.target.closest('.tag-img-lightbox')) return
  tagHelpOpen.value = false
  tagHelpPinned = false
}
onMounted(() => document.addEventListener('click', onDocClickCloseTagHelp))
onUnmounted(() => document.removeEventListener('click', onDocClickCloseTagHelp))

// AI 标题优化
const titleOptVisible = ref(false)
const currentPlatform = ref('wechat')
const selectedTitle = ref('')
const titleOptLoading = ref(false)
const optimizedTitles = ref({})

// 贴图生成弹窗（复用 CardsModal 组件）
const cardsModalVisible = ref(false)

// 编辑态
const isEditing = ref(false)
const blocks = ref([])
const modifiedIndices = ref(new Set())
const articleSnapshot = ref(null)


const platformTabMeta = [
  { key: 'wechat', label: '公众号' },
  { key: 'xiaohongshu', label: '小红书' },
  { key: 'toutiao', label: '头条' },
  { key: 'baijiahao', label: '百家号' },
  { key: 'zhihu', label: '知乎' },
  { key: 'douyin', label: '抖音' },
  { key: 'bilibili', label: 'B站' }
]

const currentPlatformTitles = computed(() => optimizedTitles.value[currentPlatform.value] || [])

// 加载文章：从 route.params.bizNo 回源拉取，不再走 localStorage
const loadArticle = async () => {
  const bizNo = route.params.bizNo
  if (!bizNo) return
  try {
    const fresh = await getArticle(bizNo)
    if (!fresh) return
    article.value = {
      id: fresh.bizNo,
      title: fresh.title,
      body: fresh.body,
      wordCount: fresh.wordCount,
      completedAt: fresh.completedAt,
      style: fresh.style,
      platform: fresh.platform,
      template: fresh.template,
      styleOverrides: fresh.styleOverrides
    }
    // 发布描述 + 推荐标签：pipeline 第 13 阶段 AI 生成，随文章落库
    publishDesc.value = fresh.description || ''
    publishTags.value = fresh.tags || []
    // 标签用法弹框默认选中当前文章平台
    if (fresh.platform && tagPlatforms.some(p => p.key === fresh.platform)) {
      activeTagPlatform.value = fresh.platform
    }
  } catch (e) {
    console.warn('preview 加载 article 失败', e)
    message.error('加载文章失败，请稍后重试')
  }
}

// 与 WorksIndex 一致：2026-07-15T10:30:00 → "7月15日 14:30"
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return ''
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hour = d.getHours().toString().padStart(2, '0')
  const min = d.getMinutes().toString().padStart(2, '0')
  return `${month}月${day}日 ${hour}:${min}`
}

// 编辑态
const goToEditPage = () => {
  if (!article.value?.id) return
  router.push(`/console/edit/${article.value.id}`)
}

const enterEditMode = () => {
  if (!article.value) return
  articleSnapshot.value = JSON.parse(JSON.stringify(article.value))
  blocks.value = parseBodyToBlocks(article.value.title, article.value.body)
  modifiedIndices.value = new Set()
  isEditing.value = true
}

const onBodyClick = (e) => {
  if (isEditing.value) return
  const target = e.target
  const editableTags = ['H1', 'H2', 'H3', 'P', 'LI', 'DIV']
  if (editableTags.includes(target.tagName) && target.closest('.article-body')) {
    enterEditMode()
    nextTick(() => {
      target.focus()
    })
  }
}

const onPaste = (e) => {
  e.preventDefault()
  const text = (e.clipboardData || window.clipboardData).getData('text/plain')
  document.execCommand('insertText', false, text)
}

const onBlockInput = (idx, e) => {
  blocks.value[idx].html = e.target.innerHTML
  modifiedIndices.value.add(idx)
}

const renderBlockHtml = (block) => {
  if (block.type === BLOCK_TYPES.TITLE) {
    return `<h1 style="font-size:24px;font-weight:700;margin-bottom:16px;line-height:1.4;color:#1a1a1a;">${block.html}</h1>`
  }
  if (block.type === BLOCK_TYPES.HEADING) {
    return `<h2 style="font-size:18px;font-weight:600;color:#1a1a1a;margin:24px 0 12px;">${block.html}</h2>`
  }
  if (block.type === BLOCK_TYPES.LIST_ITEM) {
    return `<li style="margin-bottom:8px;">${block.html}</li>`
  }
  if (block.type === BLOCK_TYPES.HIGHLIGHT) {
    return `<div style="background:#f6ffed;border-left:4px solid #07c160;padding:16px;margin:20px 0;border-radius:0 8px 8px 0;">${block.html}</div>`
  }
  return `<p style="margin-bottom:16px;">${block.html}</p>`
}

const saveEdit = async () => {
  const titleBlock = blocks.value.find(b => b.type === BLOCK_TYPES.TITLE)
  if (!titleBlock || !stripHtml(titleBlock.html).trim()) {
    message.error('标题不能为空')
    return
  }

  const { title, body } = serializeBlocksToArticle(blocks.value)

  // 直接落库，不走 localStorage
  try {
    await updateArticle(article.value.id, { title, body })
  } catch (e) {
    message.error(e?.message || '保存失败，请稍后重试')
    return
  }

  article.value = { ...article.value, title, body }
  isEditing.value = false
  modifiedIndices.value = new Set()
  message.success('内容已保存')
}

const cancelEdit = () => {
  if (articleSnapshot.value) {
    article.value = articleSnapshot.value
  }
  isEditing.value = false
  modifiedIndices.value = new Set()
  blocks.value = []
}

function stripHtml(html) {
  if (!html) return ''
  const tmp = document.createElement('div')
  tmp.innerHTML = html
  return tmp.textContent || tmp.innerText || ''
}

// 当前文章的模板视觉预设（API 加载，单例缓存）
const { getByKey, getStyle, getSignature, allSignatureTexts, load: loadExportTemplates } = useExportTemplates()
const templateStyle = computed(() => getStyle(article.value?.template) || DEFAULT_TEMPLATE_STYLE)
const templateMeta = computed(() => getByKey(article.value?.template))

// 从 body 尾部/头部剥离已知的平台签名
const stripSignatures = (body) => {
  if (!body) return body
  let result = body
  for (const sig of allSignatureTexts.value) {
    // 尾部签名
    while (result.trimEnd().endsWith(sig)) {
      result = result.trimEnd().slice(0, result.trimEnd().length - sig.length).trimEnd()
    }
    // 头部签名（zhihu）
    if (result.trimStart().startsWith(sig)) {
      result = result.trimStart().slice(sig.length).trimStart()
    }
  }
  return result
}

// styleOverrides 后端返回对象；防御旧数据/异常返回 JSON 字符串的情况
const parseStyleOverrides = (raw) => {
  if (!raw) return null
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw)
    } catch {
      return null
    }
  }
  return raw
}

// 格式化正文
const formattedBody = computed(() => {
  if (!article.value?.body) return ''
  const s = templateStyle.value
  // 防御性剥离：旧数据 body 开头可能仍含 title（后端 ExportRenderStep 曾把 title 塞进 body）
  let body = stripLeadingTitle(article.value.body, (article.value.title || '').trim())
  // 剥离已有的平台签名（pipeline 写死 wechat_default 塞入的 — 完 —）
  body = stripSignatures(body)
  // 按段落（\n\n）切分，逐段判断类型；## → h2，### → h3，【...】 → h2（旧格式），其余 → <p>
  const rendered = body.split(/\n\n+/).map(part => {
    const trimmed = part.trim()
    if (!trimmed) return ''
    const mdHeading = trimmed.match(/^(#{1,6})\s+(.+)$/)
    if (mdHeading) {
      const level = Math.min(mdHeading[1].length, 3)
      const borderCss = s.headingBorder && s.headingBorder !== 'none' ? `border-left: ${s.headingBorder}; padding-left: ${s.headingPl || '0'};` : ''
      const borderBottomCss = s.headingBorderBottom ? `border-bottom: ${s.headingBorderBottom}; padding-bottom: 6px;` : ''
      const alignCss = s.headingAlign ? `text-align: ${s.headingAlign};` : ''
      return `<h${level} style="font-size: ${s.headingSize}; font-weight: 600; color: ${s.headingColor}; margin: 18px 0 8px; ${borderCss} ${borderBottomCss} ${alignCss}">${mdHeading[2]}</h${level}>`
    }
    const legacyHeading = trimmed.match(/^【([^】]+)】$/)
    if (legacyHeading) {
      const borderCss = s.headingBorder && s.headingBorder !== 'none' ? `border-left: ${s.headingBorder}; padding-left: ${s.headingPl || '0'};` : ''
      const borderBottomCss = s.headingBorderBottom ? `border-bottom: ${s.headingBorderBottom}; padding-bottom: 6px;` : ''
      const alignCss = s.headingAlign ? `text-align: ${s.headingAlign};` : ''
      return `<h2 style="font-size: ${s.headingSize}; font-weight: 600; color: ${s.headingColor}; margin: 18px 0 8px; ${borderCss} ${borderBottomCss} ${alignCss}">${legacyHeading[1]}</h2>`
    }
    if (trimmed.startsWith('> ')) {
      const calloutText = trimmed.slice(2)
      if (s.calloutVariant === 'pill') {
        return `<div style="background: #fff0f2; padding: 8px 14px; color: #ff2442; font-size: 13px; line-height: 1.6; border-radius: 20px; margin: 14px 0; display: inline-block;">${calloutText}</div>`
      }
      if (s.calloutVariant === 'card') {
        return `<div style="background: #fff; padding: 12px 14px; color: #262626; font-size: 13px; line-height: 1.6; border-radius: 8px; margin: 14px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.08); border-left: 3px solid #07c160;">${calloutText}</div>`
      }
      if (s.calloutVariant === 'cta') {
        return `<div style="background: #fff; padding: 12px 14px; color: #262626; font-size: 13px; line-height: 1.6; border-radius: 6px; margin: 14px 0; border: 2px solid #cf1322; text-align: center;">${calloutText}</div>`
      }
      if (s.calloutVariant === 'checklist') {
        return `<div style="background: #f6ffed; padding: 12px 14px; color: #262626; font-size: 13px; line-height: 1.9; border-radius: 6px; margin: 14px 0;">${calloutText}</div>`
      }
      const borderStyle = s.calloutBorder && s.calloutBorder !== 'none' ? `border-left: ${s.calloutBorder};` : 'border: none;'
      return `<div style="background: ${s.calloutBg || '#f6ffed'}; ${borderStyle} padding: 12px 14px; color: ${s.calloutColor || '#262626'}; font-size: 13px; line-height: 1.6; border-radius: 0 6px 6px 0; margin: 14px 0;">${calloutText}</div>`
    }
    const alignCss = s.bodyAlign ? `text-align: ${s.bodyAlign};` : ''
    return `<p style="margin-bottom: 16px; font-size: ${s.bodySize}; line-height: ${s.bodyLine}; color: ${s.bodyColor}; ${alignCss}">${trimmed.replace(/\n/g, '<br>')}</p>`
  }).filter(Boolean).join('')

  // 应用编辑页保存的样式覆盖（加粗/颜色/对齐/字号等）；顶层元素按 \n\n 逐段生成，与保存时的块索引一致
  const overrides = parseStyleOverrides(article.value?.styleOverrides)
  const renderedWithOverrides = overrides ? applyStyleOverrides(rendered, overrides) : rendered

  // 追加模板对应平台的签名
  const sig = getSignature(article.value?.template)
  if (sig) {
    const sigHtml = `<div style="text-align: center; color: ${s.metaColor}; font-size: 13px; margin-top: 32px; padding-top: 16px; border-top: 1px solid ${s.metaBorder};">${sig.text}</div>`
    if (sig.position === 'start') {
      return sigHtml + renderedWithOverrides
    }
    return renderedWithOverrides + sigHtml
  }
  return renderedWithOverrides
})

// 复制正文
const copyText = () => {
  if (!article.value) return
  const text = `${article.value.title}\n\n${article.value.body}`
  navigator.clipboard.writeText(text).then(() => {
    message.success('已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}

// 导出 Word
const exportWord = () => {
  if (!article.value) return

  const title = article.value.title || '未命名文章'
  const bodyHtml = formattedBody.value

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
  a.download = title.replace(/[\\/:*?"<>|]/g, '_') + '.doc'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)

  message.success('Word 导出成功')
}

// 复制描述
const copyDesc = () => {
  navigator.clipboard.writeText(publishDesc.value).then(() => {
    message.success('描述已复制')
  }).catch(() => {
    message.error('复制失败')
  })
}

// 复制全部标签
const copyTags = () => {
  navigator.clipboard.writeText(publishTags.value.join(' ')).then(() => {
    message.success('标签已复制')
  }).catch(() => {
    message.error('复制失败')
  })
}

// AI优化标题：首次点击调后端大模型生成，之后后端返回首次缓存结果
const optimizeTitle = async () => {
  if (!article.value) return
  titleOptVisible.value = true
  selectedTitle.value = ''
  currentPlatform.value = platformTabMeta.some(t => t.key === article.value.platform)
    ? article.value.platform
    : 'wechat'
  titleOptLoading.value = true
  try {
    const res = await optimizeTitles(article.value.id)
    optimizedTitles.value = res.titles || {}
  } catch (e) {
    titleOptVisible.value = false
    message.error(e?.message || 'AI 标题优化失败，请稍后重试')
    return
  } finally {
    titleOptLoading.value = false
  }
}

const closeTitleOpt = () => {
  titleOptVisible.value = false
  selectedTitle.value = ''
}

const selectTitle = (title) => {
  selectedTitle.value = title
}

const confirmTitle = async () => {
  if (!selectedTitle.value || !article.value) return
  const newTitle = selectedTitle.value
  // 直接落库（标题也是文章字段），不走 localStorage
  try {
    await updateArticle(article.value.id, { title: newTitle })
  } catch (e) {
    message.error(e?.message || '标题保存失败，请稍后重试')
    return
  }
  article.value.title = newTitle
  message.success('标题已替换')
  closeTitleOpt()
}

// 生成贴图（打开复用弹框）
const generateCards = () => {
  cardsModalVisible.value = true
}


onMounted(() => {
  loadExportTemplates()
  loadArticle()
})
</script>

<style scoped>
.preview-index {
  height: 100%;
  padding: 24px;
  padding-bottom: 80px;
  overflow-y: auto;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-btn {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
}

.back-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.preview-title-text {
  flex: 1;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.2;
}

.preview-header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.action-btn.primary {
  background: #ff2442;
  border-color: #ff2442;
  color: #fff;
}

.action-btn.primary:hover {
  background: #e61e3a;
  border-color: #e61e3a;
}

.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 16px;
}

.empty-btn {
  padding: 8px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.empty-btn:hover {
  background: #e61e3a;
}

.preview-content {
  max-width: 680px;
  margin: 0 auto;
}

.preview-article {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 32px;
}

.article-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 16px;
  line-height: 1.4;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
  flex-wrap: wrap;
}

.article-style-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #fff0f2;
  color: #ff2442;
  border: 1px solid #ffccc7;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.article-template-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #f0f5ff;
  color: #1677ff;
  border: 1px solid #bae0ff;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.article-body {
  font-size: 16px;
  line-height: 1.8;
  color: #262626;
}

.article-body h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 24px 0 12px;
}

.publish-meta-card {
  max-width: 680px;
  margin: 32px auto 0;
  padding-top: 28px;
  border-top: 1px solid #f0f0f0;
}

.meta-section-title {
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  font-size: 15px;
}

.tag-help-icon {
  margin-left: 6px;
  color: #bfbfbf;
  font-size: 14px;
  cursor: pointer;
  vertical-align: -1px;
}

.tag-help-icon:hover {
  color: #ff2442;
}

.tag-help-panel {
  width: 320px;
}

.tag-help-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.tag-help-tab {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: #595959;
  background: #f5f5f5;
  cursor: pointer;
  transition: all 0.15s;
}

.tag-help-tab:hover {
  color: #ff2442;
}

.tag-help-tab.active {
  background: #ff2442;
  color: #fff;
}

/* 步骤区写死高度：切平台 tab 时弹框不跳动，内容超高内部滚动 */
.tag-help-steps {
  height: 108px;
  overflow-y: auto;
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  line-height: 1.7;
  color: #262626;
}

.tag-help-steps li {
  margin-bottom: 4px;
}

/* 操作示意图：固定高度，切平台 tab 时弹框不跳动 */
.tag-help-img-wrap {
  position: relative;
  margin-top: 8px;
  cursor: zoom-in;
}

.tag-help-img {
  display: block;
  width: 100%;
  height: 150px;
  object-fit: cover;
  object-position: top;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}

.tag-help-img-hint {
  position: absolute;
  right: 6px;
  bottom: 6px;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  pointer-events: none;
}

/* 示意图放大查看：整屏遮罩 + 原图，点任意处关闭 */
.tag-img-lightbox {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(0, 0, 0, 0.78);
  cursor: zoom-out;
}

.tag-img-lightbox-img {
  max-width: 92vw;
  max-height: 84vh;
  border-radius: 8px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.4);
}

.tag-img-lightbox-tip {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
}

.tag-img-fade-enter-active,
.tag-img-fade-leave-active {
  transition: opacity 0.18s ease;
}

.tag-img-fade-enter-from,
.tag-img-fade-leave-to {
  opacity: 0;
}

.tag-help-footer {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #8c8c8c;
}

.publish-desc-input {
  width: 100%;
  min-height: 90px;
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

.publish-desc-input:focus {
  outline: none;
  border-color: #ff2442;
}

.meta-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.meta-btn {
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.meta-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.meta-btn.primary {
  background: #fff;
  border-color: #ff2442;
  color: #ff2442;
}

.meta-btn.primary:hover {
  background: #fff0f2;
}

.publish-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.publish-tag {
  padding: 4px 12px;
  background: #f5f5f5;
  border-radius: 14px;
  font-size: 13px;
  color: #595959;
}

/* 浮动操作栏 */
.floating-action-bar {
  position: fixed;
  bottom: 0;
  left: 200px;
  right: 0;
  background: #fff;
  border-top: 1px solid #eee;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
  padding: 10px 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  z-index: 100;
}

.float-btn {
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.15s;
}

.float-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.float-btn.primary {
  background: #ff2442;
  border-color: #ff2442;
  color: #fff;
}

.float-btn.primary:hover {
  background: #e61e3a;
}

.float-btn.outline {
  background: #fff;
  border-color: #ff2442;
  color: #ff2442;
}

.float-btn.outline:hover {
  background: #fff0f2;
}

.float-btn.danger {
  background: #fff;
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.float-btn.danger:hover {
  background: #fff2f0;
}

/* AI 标题优化弹窗 */
.title-opt-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 1000;
}

.title-opt-box {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 560px;
  max-height: 86vh;
  overflow-y: auto;
  padding: 28px;
  position: relative;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.title-opt-close {
  position: absolute;
  top: 12px;
  right: 16px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  font-size: 24px;
  color: #8c8c8c;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.title-opt-close:hover {
  color: #262626;
  background: #f5f5f5;
}

.title-opt-header {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 20px;
}

.title-opt-section {
  margin-bottom: 24px;
}

.title-opt-original {
  font-size: 13px;
  color: #8c8c8c;
  background: #fff0f2;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 16px;
  line-height: 1.5;
}

.title-opt-original span {
  color: var(--color-primary);
  font-weight: 600;
  margin-right: 8px;
}

.title-opt-section-label {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title-opt-loading {
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: #8c8c8c;
}

.title-opt-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.title-opt-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.title-opt-item:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.title-opt-item.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.title-opt-radio {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #d9d9d9;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.title-opt-item.selected .title-opt-radio {
  border-color: var(--color-primary);
  background: var(--color-primary);
}

.title-opt-item.selected .title-opt-radio::after {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
}

.title-opt-item-text {
  flex: 1;
  font-size: 14px;
  color: #262626;
  line-height: 1.5;
}

.title-opt-platform-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.title-opt-platform-tab {
  padding: 6px 14px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 16px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.15s;
  font-weight: 500;
}

.title-opt-platform-tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.title-opt-platform-tab.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.title-opt-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.title-opt-btn-cancel {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.title-opt-btn-cancel:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.title-opt-btn-confirm {
  padding: 8px 20px;
  background: var(--color-primary);
  border: none;
  border-radius: 6px;
  font-size: 13px;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.title-opt-btn-confirm:hover {
  background: var(--color-primary-hover);
}

.title-opt-btn-confirm:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.title-opt-fade-enter-active,
.title-opt-fade-leave-active {
  transition: opacity 0.2s ease;
}

.title-opt-fade-enter-from,
.title-opt-fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .title-opt-box {
    padding: 20px 16px 16px;
    border-radius: 12px;
    max-height: 80vh;
  }

  .title-opt-header {
    font-size: 16px;
  }
}

.title-opt-list .title-opt-item {
  animation: title-opt-item-in 0.3s ease both;
}

@keyframes title-opt-item-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}


.editing-body .edit-block {
  outline: none;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 4px;
  transition: border-color 0.2s;
}

.editing-body .edit-block:focus,
.editing-body .edit-block:focus-within {
  border-color: #07c160;
  box-shadow: 0 0 0 2px rgba(7, 193, 96, 0.15);
}

.editing-body .edit-block.modified {
  background: #f6ffed;
}

.edit-floating-bar {
  position: fixed;
  bottom: 0;
  left: 200px;
  right: 0;
  background: #fff;
  border-top: 1px solid #eee;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
  padding: 10px 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  z-index: 100;
}

.edit-hint {
  font-size: 13px;
  color: #595959;
  margin-right: 8px;
}

@media (max-width: 768px) {
  .preview-index {
    padding: 16px 12px 160px;
  }

  .preview-header {
    flex-wrap: wrap;
    gap: 10px;
  }

  .preview-title-text {
    font-size: 18px;
    flex-basis: 100%;
    order: 1;
  }

  .back-btn {
    display: none;
  }

  .preview-header-actions {
    order: 2;
    width: 100%;
    gap: 6px;
    flex-wrap: wrap;
  }

  .preview-header-actions .action-btn {
    flex: 1;
    min-width: 0;
    justify-content: center;
    padding: 6px 8px;
    font-size: 12px;
  }

  .preview-article {
    padding: 18px 16px;
  }

  .article-title {
    font-size: 20px;
  }

  .article-body {
    font-size: 15px;
  }

  .publish-meta-card {
    margin-top: 24px;
    padding-top: 20px;
  }

  /* 浮动操作栏在手机端占据底部，左=0，叠加在 tabbar 之上 */
  .floating-action-bar {
    left: 0;
    right: 0;
    bottom: 60px;
    gap: 6px;
    padding: 8px 12px;
    overflow-x: auto;
    flex-wrap: nowrap;
    justify-content: flex-start;
    scrollbar-width: none;
  }

  .floating-action-bar::-webkit-scrollbar {
    display: none;
  }

  .floating-action-bar .float-btn {
    flex-shrink: 0;
    padding: 6px 12px;
    font-size: 12px;
    white-space: nowrap;
  }

  .edit-floating-bar {
    left: 0;
    right: 0;
    bottom: 60px;
    padding: 8px 12px;
    gap: 6px;
  }

  .edit-hint {
    font-size: 11px;
    flex: 1;
    min-width: 0;
  }

  /* 弹框在窄屏下压低高度 */
  .title-opt-box {
    padding: 18px;
  }
}

/* 深色模式 */
body[data-theme="dark"] .back-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .tag-help-icon {
  color: #737373;
}

body[data-theme="dark"] .tag-help-icon:hover {
  color: var(--color-primary);
}

body[data-theme="dark"] .tag-help-tab {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .tag-help-tab:hover {
  color: var(--color-primary);
}

body[data-theme="dark"] .tag-help-tab.active {
  background: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .tag-help-steps {
  color: #d9d9d9;
}

body[data-theme="dark"] .tag-help-footer {
  border-top-color: #303030;
  color: #737373;
}

body[data-theme="dark"] .tag-help-img {
  border-color: #303030;
}

body[data-theme="dark"] .back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .preview-title-text {
  color: #f0f0f0;
}

body[data-theme="dark"] .action-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .action-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .action-btn.primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .action-btn.primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

body[data-theme="dark"] .empty-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .preview-article {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .article-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .article-meta {
  color: #a6a6a6;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .article-style-badge {
  background: rgba(7, 193, 96, 0.15);
  border-color: rgba(7, 193, 96, 0.35);
  color: #4ade80;
}

body[data-theme="dark"] .article-template-badge {
  background: rgba(22, 119, 255, 0.15);
  border-color: rgba(22, 119, 255, 0.35);
  color: #69b1ff;
}

body[data-theme="dark"] .article-body {
  color: #d9d9d9;
}

body[data-theme="dark"] .article-body h2 {
  color: #f0f0f0;
}

body[data-theme="dark"] .publish-meta-card {
  border-top-color: #303030;
}

body[data-theme="dark"] .meta-section-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .publish-desc-input {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .publish-desc-input:focus {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .meta-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .meta-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .meta-btn.primary {
  background: transparent;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .meta-btn.primary:hover {
  background: rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .publish-tag {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .floating-action-bar {
  background: #1f1f1f;
  border-top-color: #303030;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.4);
}

body[data-theme="dark"] .float-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .float-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .float-btn.primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .float-btn.primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

body[data-theme="dark"] .float-btn.outline {
  background: transparent;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .float-btn.outline:hover {
  background: rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .float-btn.danger {
  background: transparent;
  border-color: #ff4d4f;
  color: #ff4d4f;
}

body[data-theme="dark"] .float-btn.danger:hover {
  background: rgba(255, 77, 79, 0.15);
}

body[data-theme="dark"] .edit-floating-bar {
  background: #1f1f1f;
  border-color: #303030;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.4);
}

body[data-theme="dark"] .edit-hint {
  color: #a6a6a6;
}

/* AI 标题优化弹框（自定义，被 teleport 到 body） */
body[data-theme="dark"] .title-opt-overlay {
  background: rgba(0, 0, 0, 0.6);
}

body[data-theme="dark"] .title-opt-box {
  background: #1f1f1f;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .title-opt-close {
  color: #a6a6a6;
}

body[data-theme="dark"] .title-opt-close:hover {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-header {
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-original {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-original span {
  color: #a6a6a6;
}

body[data-theme="dark"] .title-opt-section-label {
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-loading {
  color: #a6a6a6;
}

body[data-theme="dark"] .title-opt-item {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .title-opt-item:hover {
  background: #2a2a2a;
  border-color: var(--color-primary);
}

body[data-theme="dark"] .title-opt-item.selected {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .title-opt-radio {
  border-color: #434343;
}

body[data-theme="dark"] .title-opt-item.selected .title-opt-radio {
  border-color: var(--color-primary);
  background: var(--color-primary);
  box-shadow: inset 0 0 0 3px #1f1f1f;
}

body[data-theme="dark"] .title-opt-item-text {
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-platform-tab {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .title-opt-platform-tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .title-opt-platform-tab.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .title-opt-footer {
  border-top-color: #303030;
}

body[data-theme="dark"] .title-opt-btn-cancel {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .title-opt-btn-cancel:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .title-opt-btn-confirm {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .title-opt-btn-confirm:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

body[data-theme="dark"] .title-opt-btn-confirm:disabled {
  background: #434343;
  border-color: #434343;
  color: #737373;
}

body[data-theme="dark"] .editing-body .edit-block {
  border-color: #434343;
}
</style>

<style>
/* 标签用法弹框 chrome 被 teleport 到 body，深色适配需全局覆盖 */
body[data-theme="dark"] .tag-help-popover .ant-popover-inner {
  background: #1f1f1f;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .tag-help-popover .ant-popover-arrow-content {
  --antd-arrow-background-color: #1f1f1f;
  background: #1f1f1f;
}
</style>