/**
 * Tipos de lista disponíveis
 */
export enum ListTypeEnum {
  SHOPPING = 1,
  TASK = 2,
  WISHLIST = 3,
  GENERIC = 4,
}

/**
 * Informações sobre um tipo de lista
 */
export interface ListType {
  id: number
  name: string
  slug: string
}

/**
 * Informações sobre o dono de uma lista
 */
export interface ListOwner {
  id: string
  username: string
  name: string
  avatarUrl: string | null
}

/**
 * Resposta da API ao obter/criar uma lista
 */
export interface ListResponse {
  id: string
  name: string
  type: ListType
  owner: ListOwner
  inviteCode: string
  isOwner: boolean
  itemsCount: number
  createdAt: string
  updatedAt: string
}

export interface ListStateResponse {
  listId: string
  revision: number
  updatedAt: string
  itemsCount: number
}

/**
 * Request para criar uma nova lista
 */
export interface CreateListRequest {
  name: string
  typeId: number
}

/**
 * Resposta da API ao gerar link de convite
 */
export interface InviteLinkResponse {
  inviteCode: string
  inviteLink: string
  expiresAt: string
}

/**
 * Item de lista no modo read-only (via convite)
 */
export interface JoinListItem {
  id: string
  name: string
  checked: boolean
  quantity: number | null
  dueDate: string | null
  url: string | null
  position: number
}

/**
 * Resposta da API ao visualizar lista via convite (modo read-only)
 */
export interface JoinListResponse {
  id: string
  name: string
  typeSlug: string
  typeName: string
  ownerUsername: string
  ownerName: string
  ownerAvatarUrl: string | null
  items: JoinListItem[]
  inviteCode: string
  expiresAt: string
  mode: 'READ_ONLY'
}

/**
 * Resposta da API ao entrar em uma lista via convite (autenticado)
 * Retorna 201 para novo membro, 200 para membro existente
 */
export interface ListJoinedResponse {
  id: string
  name: string
  typeSlug: string
  typeName: string
  role: 'OWNER' | 'MEMBER'
  message: string
  created: boolean
  list: ListResponse
}

export interface UserSearchResult {
  username: string
  name: string
  avatarUrl: string | null
}

export interface InviteByUsernameResponse {
  invitedUsername: string
  message: string
}

export interface ListMemberUser {
  id: string
  username: string
  name: string
  avatarUrl: string | null
}

export interface ListMemberResponse {
  user: ListMemberUser
  role: 'OWNER' | 'MEMBER'
  joinedAt: string
}

/**
 * Definições dos tipos de lista com emoji e descrição
 */
export const LIST_TYPES = [
  {
    id: 1,
    name: 'Compras',
    slug: 'compras',
    emoji: '🛒',
    description: 'Lista de compras do mercado',
  },
  {
    id: 2,
    name: 'Tarefas',
    slug: 'tarefas',
    emoji: '✅',
    description: 'Lista de tarefas a fazer',
  },
  {
    id: 3,
    name: 'Wishlist',
    slug: 'wishlist',
    emoji: '🎁',
    description: 'Lista de desejos',
  },
  {
    id: 4,
    name: 'Genérica',
    slug: 'generica',
    emoji: '📝',
    description: 'Lista genérica',
  },
] as const
