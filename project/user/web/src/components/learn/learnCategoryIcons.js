import {
  AimOutlined,
  RocketOutlined,
  ThunderboltOutlined,
  RiseOutlined,
  IdcardOutlined,
  WalletOutlined
} from '@ant-design/icons-vue'

/**
 * 创作学院顶级分类 → Ant Design 图标映射。
 * 找不到映射时不渲染图标（fallback 到 caret 占位）。
 */
export const CATEGORY_ICONS = {
  '内容定位': AimOutlined,
  '平台运营技巧': RocketOutlined,
  '爆款方法论': ThunderboltOutlined,
  '涨粉与流量增长': RiseOutlined,
  'IP 打造与人设': IdcardOutlined,
  '变现路径': WalletOutlined
}
