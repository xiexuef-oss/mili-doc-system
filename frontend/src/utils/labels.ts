/** Military security classification labels. 4 duplicate copies eliminated. */
export function securityLabel(level: string | null | undefined): string {
  const map: Record<string, string> = {
    PUBLIC: '公开', INTERNAL: '内部', SECRET: '秘密',
    CONFIDENTIAL: '机密', TOP_SECRET: '绝密',
  }
  return level ? (map[level] ?? level) : ''
}

/** Project status labels. 3 duplicate copies eliminated. */
export function projectStatusLabel(status: string | null | undefined): string {
  const map: Record<string, string> = {
    DRAFT: '草稿', IN_PROGRESS: '进行中', COMPLETED: '已完成', ARCHIVED: '已归档',
  }
  return status ? (map[status] ?? status) : ''
}

/** Project status tag type (Element Plus el-tag type). */
export function projectStatusTagType(status: string | null | undefined): string {
  const map: Record<string, string> = {
    DRAFT: 'info', IN_PROGRESS: 'primary', COMPLETED: 'success', ARCHIVED: 'warning',
  }
  return status ? (map[status] ?? 'info') : 'info'
}

/** Stage status labels. 2 duplicate copies eliminated. */
export function stageStatusLabel(status: string | null | undefined): string {
  const map: Record<string, string> = {
    NOT_STARTED: '未开始', PLANNING: '规划中', IN_PROGRESS: '进行中',
    REVIEWING: '评审中', RECTIFYING: '整改中', BASELINING: '基线化',
    GATE_CHECKING: '转阶段检查', COMPLETED: '已完成',
    SUSPENDED: '暂停', TERMINATED: '终止',
  }
  return status ? (map[status] ?? status) : ''
}

/** Stage status tag type. */
export function stageStatusTagType(status: string | null | undefined): string {
  const map: Record<string, string> = {
    NOT_STARTED: '', PLANNING: 'info', IN_PROGRESS: 'warning',
    REVIEWING: 'warning', RECTIFYING: 'danger', BASELINING: 'primary',
    GATE_CHECKING: 'info', COMPLETED: 'success',
    SUSPENDED: 'danger', TERMINATED: 'info',
  }
  return status ? (map[status] ?? 'info') : 'info'
}

/** Doc lifecycle status labels. */
export function docLifecycleStatusLabel(status: string | null | undefined): string {
  const map: Record<string, string> = {
    PLANNED: '策划', DRAFTING: '起草', CHECKING: '校对',
    REVIEWING: '评审', APPROVING: '批准', RELEASED: '已发布', ARCHIVED: '已归档',
  }
  return status ? (map[status] ?? status) : ''
}

/** Doc lifecycle tag type. */
export function docLifecycleTagType(status: string | null | undefined): string {
  const map: Record<string, string> = {
    PLANNED: 'info', DRAFTING: 'info', CHECKING: 'warning',
    REVIEWING: 'warning', APPROVING: 'warning',
    RELEASED: 'success', ARCHIVED: 'info',
  }
  return status ? (map[status] ?? 'info') : 'info'
}

/** Meeting type labels. 2 duplicate copies eliminated. */
export function meetingTypeLabel(type: string | null | undefined): string {
  const map: Record<string, string> = {
    DESIGN_REVIEW: '设计评审', TECH_REVIEW: '技术评审',
    QUALITY_REVIEW: '质量评审', STAGE_REVIEW: '阶段评审', FINAL_REVIEW: '终审评审',
  }
  return type ? (map[type] ?? type) : ''
}

/** Baseline type labels. 2 duplicate copies eliminated. */
export function baselineTypeLabel(type: string | null | undefined): string {
  const map: Record<string, string> = {
    FUNCTIONAL_BASELINE: '功能基线', ALLOCATED_BASELINE: '分配基线',
    PRODUCT_BASELINE: '产品基线',
  }
  return type ? (map[type] ?? type) : ''
}
