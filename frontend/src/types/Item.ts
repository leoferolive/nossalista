/**
 * Informações sobre o criador de um item
 */
export interface CreatorInfo {
  id: string
  username: string
  name: string
  avatarUrl: string | null
}

/**
 * Representa um item de lista na API
 */
export interface ListItem {
  id: string
  name: string
  checked: boolean
  quantity: number | null
  dueDate: string | null // ISO 8601
  url: string | null
  position: number
  createdBy: CreatorInfo
  createdAt: string // ISO 8601
  updatedAt: string // ISO 8601
}

/**
 * Request para criar um novo item
 */
export interface CreateItemRequest {
  name: string
  quantity?: number | null
  dueDate?: string | null
  url?: string | null
}

/**
 * Request para atualizar um item existente
 */
export interface UpdateItemRequest {
  name: string
  quantity?: number
  dueDate?: string
  url?: string
}

/**
 * Props para o componente ListItem
 */
export interface ListItemProps {
  item: ListItem
  onToggle: (id: string) => void
  onEdit: (item: ListItem) => void
  onDelete?: (item: ListItem) => void
  isDeleting?: boolean
  isWsAdded?: boolean
  isWsChecked?: boolean
  isWsCheckedHighlight?: boolean
}
