/**
 * 工作台操作向导（driver.js 遮罩高亮引导）。
 *
 * 首次进入工作台自动弹出一次（按用户隔离记录），
 * 之后可通过工作台「操作指引」入口手动重看。
 *
 * 步骤通过元素上的 data-guide 属性定位：
 *  - ConsoleLayout：侧边栏导航项（PC）
 *  - WorkbenchIndex：创作按钮、运营方案、快捷操作、工具箱、生成记录等
 * 手机端侧边栏隐藏，会自动过滤不存在的步骤。
 */
import { driver } from 'driver.js'
import 'driver.js/dist/driver.css'
import { STORAGE_KEYS, getWorkbenchGuideDoneKey } from '@/constants/storage.js'

function isMobileView() {
  return window.innerWidth <= 768
}

// PC 端步骤（含侧边栏）
const desktopSteps = [
  {
    element: '[data-guide="nav-workbench"]',
    popover: {
      title: '工作台',
      description: '您的创作大本营，每天从这里开始，快速了解账户和今日任务。'
    }
  },
  {
    element: '[data-guide="nav-commission"]',
    popover: {
      title: '约稿中心',
      description: '接平台约稿任务，按要求创作即可赚取创作币。'
    }
  },
  {
    element: '[data-guide="nav-skill-market"]',
    popover: {
      title: '提示词市场',
      description: '浏览、使用优质提示词，让 AI 写出更贴合您风格的文章。'
    }
  },
  {
    element: '[data-guide="nav-mine"]',
    popover: {
      title: '我的',
      description: '我的作品、账户、权益、订单、自媒体账号，都收在这里。'
    }
  },
  {
    element: '[data-guide="create-btn"]',
    popover: {
      title: '开始今日创作',
      description: '最核心的入口：点击后选择平台和提示词，AI 帮您一键成文。'
    }
  },
  {
    element: '[data-guide="plan-card"]',
    popover: {
      title: '运营方案',
      description: '专属顾问小爱为您定制主攻平台、赛道和人设，创作前先定好方向。'
    }
  },
  {
    element: '[data-guide="shortcut-card"]',
    popover: {
      title: '快捷操作',
      description: '常用功能一步直达：账号检测、热搜榜、邀请有礼等。'
    }
  },
  {
    element: '[data-guide="toolbox-card"]',
    popover: {
      title: '创作工具箱',
      description: '违禁词检测、标题优化等实用小工具，写作前后都能用。'
    }
  },
  {
    element: '[data-guide="generation-card"]',
    popover: {
      title: '生成记录',
      description: '最近 7 天的创作任务都在这里，点击查看、继续或下载成稿。'
    }
  }
]

// 手机端步骤（无侧边栏，突出底部导航）
const mobileSteps = [
  {
    element: '[data-guide="create-btn"]',
    popover: {
      title: '开始今日创作',
      description: '最核心的入口：点击后选择平台和提示词，AI 帮您一键成文。'
    }
  },
  {
    element: '[data-guide="feature-bar"]',
    popover: {
      title: '约稿中心 / 提示词市场',
      description: '接任务赚创作币、用提示词提升质量，点这里快速进入。'
    }
  },
  {
    element: '[data-guide="plan-card"]',
    popover: {
      title: '运营方案',
      description: '专属顾问小爱为您定制主攻平台、赛道和人设，创作前先定好方向。'
    }
  },
  {
    element: '[data-guide="toolbox-card"]',
    popover: {
      title: '创作工具箱',
      description: '违禁词检测、标题优化等实用小工具，写作前后都能用。'
    }
  },
  {
    element: '[data-guide="generation-card"]',
    popover: {
      title: '生成记录',
      description: '最近 7 天的创作任务都在这里，点击查看、继续或下载成稿。'
    }
  },
  {
    element: '[data-guide="tabbar"]',
    popover: {
      title: '底部导航',
      description: '控制台、创作学院、消息、我的四个高频入口常驻底部，随时切换。'
    }
  }
]

let driverObj = null

function getGuideDone() {
  const userId = localStorage.getItem(STORAGE_KEYS.USER_ID)
  return localStorage.getItem(getWorkbenchGuideDoneKey(userId))
}

function markGuideDone() {
  const userId = localStorage.getItem(STORAGE_KEYS.USER_ID)
  localStorage.setItem(getWorkbenchGuideDoneKey(userId), '1')
}

function buildDriver() {
  // 只保留当前页面实际存在的步骤，避免高亮空目标
  const allSteps = isMobileView() ? mobileSteps : desktopSteps
  const steps = allSteps.filter((s) => document.querySelector(s.element))
  if (!steps.length) return null

  driverObj = driver({
    steps,
    showProgress: true,
    progressText: '{{current}} / {{total}}',
    nextBtnText: '下一步',
    prevBtnText: '上一步',
    doneBtnText: '完成',
    popoverClass: 'wb-guide-popover',
    stagePadding: 6,
    stageRadius: 10,
    onCloseClick: () => {
      markGuideDone()
      driverObj?.destroy()
    },
    onDestroyed: () => {
      markGuideDone()
      driverObj = null
    }
  })
  return driverObj
}

export function useWorkbenchGuide() {
  /**
   * 手动重看（工作台「操作指引」入口），始终弹出。
   */
  const startGuide = () => {
    const d = buildDriver()
    d?.drive()
  }

  /**
   * 首次进入自动弹出；已看过则跳过。
   * @param {object} options
   * @param {boolean} options.blocked 页面有其他自动弹窗（如运营方案弹窗）时传 true，避免叠弹
   */
  const startGuideIfFirstVisit = ({ blocked = false } = {}) => {
    if (blocked) return
    if (getGuideDone()) return
    startGuide()
  }

  return { startGuide, startGuideIfFirstVisit }
}
