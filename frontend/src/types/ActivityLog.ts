/**
 * Ações de atividade registradas no sistema
 */
export type ActivityAction =
  | 'ITEM_ADDED'
  | 'ITEM_CHECKED'
  | 'ITEM_UNCHECKED'
  | 'ITEM_UPDATED'
  | 'ITEM_REMOVED'
  | 'MEMBER_JOINED'
  | 'MEMBER_LEFT'
  | 'MEMBER_REMOVED'

/**
 * Detalhes da atividade (JSONB flexível)
 */
export interface ActivityDetails {
  oldValue?: string | number | boolean
  newValue?: string | number | boolean
  changedFields?: string[]
  method?: 'LINK' | 'USERNAME'
  invitedBy?: string
  wasKicked?: boolean
  removedBy?: string
}

/**
 * Representa um registro de atividade na API
 */
export interface ActivityLog {
  id: string
  listId: string
  userId: string
  userName: string | null // nome de exibição do usuário que executou a ação
  action: ActivityAction
  targetType: 'ITEM' | 'MEMBER'
  targetId: string | null
  targetName: string | null
  details: ActivityDetails | null
  createdAt: string // ISO 8601
}

/**
 * Response da API de atividades com paginação
 */
export interface ActivityResponse {
  content: ActivityLog[]
  totalPages: number
  totalElements: number
  size: number
  number: number
  first: boolean
  last: boolean
}

/**
 * Props para o componente ActivityTimeline
 */
export interface ActivityTimelineProps {
  activities: ActivityLog[]
  isOpen: boolean
  onClose: () => void
  onLoadMore?: () => void
  hasMore?: boolean
  loading?: boolean
}
