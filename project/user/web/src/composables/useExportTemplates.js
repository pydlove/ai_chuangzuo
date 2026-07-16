import { ref, computed } from 'vue'
import { listExportTemplates } from '@/api/exportTemplate.js'

/**
 * 导出模板 composable：创作页弹框和预览页共用。
 * 首次调用时从 API 拉取，后续用缓存。
 *
 * 每个模板对象规范化为：
 * { key, name, desc, platform, bgColor, textColor, visualStyle, signatureText, signaturePosition, sortOrder }
 */
const templates = ref([])
const loaded = ref(false)
const loading = ref(false)

/** 模板未找到 / API 未加载时的兜底视觉样式（同 wechat 种子） */
export const DEFAULT_TEMPLATE_STYLE = {
  bg: '#fff', font: '-apple-system, sans-serif',
  titleColor: '#1a1a1a', titleSize: '22px',
  metaColor: '#8c8c8c', metaBorder: '#eee',
  bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85',
  headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0',
  calloutBg: '#f6ffed', calloutBorder: '4px solid #07c160', calloutColor: '#262626'
}

/** 后端可能把 visualStyle 返回成 JSON 字符串，统一解析成对象 */
function parseVisualStyle(v) {
  if (!v) return null
  if (typeof v === 'string') {
    try { return JSON.parse(v) } catch { return null }
  }
  return v
}

export function useExportTemplates() {
  const load = async () => {
    if (loaded.value || loading.value) return
    loading.value = true
    try {
      const raw = await listExportTemplates()
      templates.value = raw.map(t => ({
        key: t.templateKey,
        name: t.name,
        desc: t.description,
        platform: t.platform,
        bgColor: t.bgColor,
        textColor: t.textColor,
        visualStyle: parseVisualStyle(t.visualStyle),
        signatureText: t.signatureText,
        signaturePosition: t.signaturePosition,
        sortOrder: t.sortOrder
      }))
      loaded.value = true
    } catch (e) {
      console.warn('加载导出模板失败', e)
    } finally {
      loading.value = false
    }
  }

  const getByKey = (key) => {
    return templates.value.find(t => t.key === key) || null
  }

  const getStyle = (key) => {
    const tpl = getByKey(key)
    return tpl?.visualStyle || null
  }

  const getSignature = (key) => {
    const tpl = getByKey(key)
    if (!tpl?.signatureText) return null
    return { text: tpl.signatureText, position: tpl.signaturePosition || 'end' }
  }

  /** 所有已知签名文本（用于从 body 中剥离） */
  const allSignatureTexts = computed(() => {
    return templates.value
      .filter(t => t.signatureText)
      .map(t => t.signatureText)
  })

  return { templates, loaded, loading, load, getByKey, getStyle, getSignature, allSignatureTexts }
}
