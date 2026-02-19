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
  id: number;
  name: string;
  slug: string;
}

/**
 * Informações sobre o dono de uma lista
 */
export interface ListOwner {
  id: string;
  username: string;
  name: string;
  avatarUrl: string | null;
}

/**
 * Resposta da API ao obter/criar uma lista
 */
export interface ListResponse {
  id: string;
  name: string;
  type: ListType;
  owner: ListOwner;
  inviteCode: string;
  isOwner: boolean;
  itemsCount: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Request para criar uma nova lista
 */
export interface CreateListRequest {
  name: string;
  typeId: number;
}

/**
 * Resposta da API ao gerar link de convite
 */
export interface InviteLinkResponse {
  invite_code: string;
  invite_link: string;
  expires_at: string;
}

/**
 * Item de lista no modo read-only (via convite)
 */
export interface JoinListItem {
  id: string;
  name: string;
  checked: boolean;
  quantity: number | null;
  due_date: string | null;
  url: string | null;
  position: number;
}

/**
 * Resposta da API ao visualizar lista via convite (modo read-only)
 */
export interface JoinListResponse {
  id: string;
  name: string;
  type_slug: string;
  type_name: string;
  owner_username: string;
  owner_name: string;
  owner_avatar_url: string | null;
  items: JoinListItem[];
  invite_code: string;
  expires_at: string;
  mode: 'READ_ONLY';
}

/**
 * Resposta da API ao entrar em uma lista via convite (autenticado)
 * Retorna 201 para novo membro, 200 para membro existente
 */
export interface ListJoinedResponse {
  id: string;
  name: string;
  type_slug: string;
  type_name: string;
  role: 'OWNER' | 'MEMBER';
  message: string;
  created: boolean;
  list: ListResponse;
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
] as const;
