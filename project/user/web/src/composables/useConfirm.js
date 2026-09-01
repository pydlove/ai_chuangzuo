import { Modal } from 'ant-design-vue'

/**
 * 统一二次确认弹框。
 *
 * 在 ant-design-vue Modal.confirm 基础上简化常用配置：
 * - 默认 `centered: true`、取消文案为「取消」。
 * - 提供 `danger` 快捷设置确认按钮为危险样式。
 * - 保留 `okButtonProps` 等其他属性用于自定义。
 */
export function useConfirm() {
  const confirm = (options = {}) => {
    const {
      danger = false,
      centered = true,
      cancelText = '取消',
      okButtonProps,
      ...rest
    } = options

    Modal.confirm({
      centered,
      cancelText,
      okButtonProps: {
        danger,
        ...okButtonProps
      },
      ...rest
    })
  }

  return { confirm }
}
