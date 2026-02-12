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
