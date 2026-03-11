import type { Connect } from 'vite'

type MockUser = {
  id: string
  username: string
  email: string
  name: string
  avatarUrl: string | null
  onboardingCompletedAt: string | null
}

type MockList = {
  id: string
  name: string
  typeId: number
  ownerId: string
  inviteCode: string
  createdAt: string
  updatedAt: string
  revision: number
  memberIds: string[]
}

type MockItem = {
  id: string
  listId: string
  name: string
  checked: boolean
  quantity: number | null
  dueDate: string | null
  url: string | null
  position: number
  createdBy: string
  createdAt: string
  updatedAt: string
}

type MockActivity = {
  id: string
  type: string
  actorName: string
  itemName?: string
  createdAt: string
}

const listTypes = [
  { id: 1, name: 'Compras', slug: 'compras' },
  { id: 2, name: 'Tarefas', slug: 'tarefas' },
  { id: 3, name: 'Wishlist', slug: 'wishlist' },
  { id: 4, name: 'Genérica', slug: 'generica' },
]

const now = () => new Date().toISOString()

function mockId(prefix: string) {
  return `${prefix}-${crypto.randomUUID()}`
}

const demoUser: MockUser = {
  id: 'user-demo',
  username: 'leo',
  email: 'leo@test.com',
  name: 'Leo Oliveira',
  avatarUrl: null,
  onboardingCompletedAt: null,
}

const shoppingListId = 'list-demo-1'
const tasksListId = 'list-demo-2'

const state = {
  users: new Map<string, MockUser>([[demoUser.id, demoUser]]),
  lists: new Map<string, MockList>([
    [
      shoppingListId,
      {
        id: shoppingListId,
        name: 'Mercado da Semana',
        typeId: 1,
        ownerId: demoUser.id,
        inviteCode: 'MOCKSHOP',
        createdAt: now(),
        updatedAt: now(),
        revision: 3,
        memberIds: [demoUser.id],
      },
    ],
    [
      tasksListId,
      {
        id: tasksListId,
        name: 'Plano de Sexta',
        typeId: 2,
        ownerId: demoUser.id,
        inviteCode: 'MOCKTASK',
        createdAt: now(),
        updatedAt: now(),
        revision: 2,
        memberIds: [demoUser.id],
      },
    ],
  ]),
  items: new Map<string, MockItem[]>([
    [
      shoppingListId,
      [
        {
          id: 'item-demo-1',
          listId: shoppingListId,
          name: 'Tomate sweet grape',
          checked: true,
          quantity: 2,
          dueDate: null,
          url: null,
          position: 0,
          createdBy: demoUser.id,
          createdAt: now(),
          updatedAt: now(),
        },
        {
          id: 'item-demo-2',
          listId: shoppingListId,
          name: 'Queijo minas',
          checked: false,
          quantity: 1,
          dueDate: null,
          url: null,
          position: 1,
          createdBy: demoUser.id,
          createdAt: now(),
          updatedAt: now(),
        },
      ],
    ],
    [
      tasksListId,
      [
        {
          id: 'item-demo-3',
          listId: tasksListId,
          name: 'Separar presentes',
          checked: false,
          quantity: null,
          dueDate: null,
          url: null,
          position: 0,
          createdBy: demoUser.id,
          createdAt: now(),
          updatedAt: now(),
        },
      ],
    ],
  ]),
  activities: new Map<string, MockActivity[]>([
    [
      shoppingListId,
      [{ id: 'activity-1', type: 'ITEM_CHECKED', actorName: 'Leo Oliveira', itemName: 'Tomate sweet grape', createdAt: now() }],
    ],
    [tasksListId, [{ id: 'activity-2', type: 'ITEM_ADDED', actorName: 'Leo Oliveira', itemName: 'Separar presentes', createdAt: now() }]],
  ]),
}

function getType(typeId: number) {
  return listTypes.find((type) => type.id === typeId) ?? listTypes[3]
}

function getUserFromRequest(req: Connect.IncomingMessage) {
  const authHeader = req.headers.authorization
  const token = authHeader?.replace(/^Bearer\s+/i, '')
  const userId = token?.startsWith('mock-token-') ? token.replace('mock-token-', '') : demoUser.id
  return state.users.get(userId) ?? demoUser
}

function buildListResponse(list: MockList, currentUser: MockUser) {
  const owner = state.users.get(list.ownerId) ?? demoUser
  return {
    id: list.id,
    name: list.name,
    type: getType(list.typeId),
    owner: {
      id: owner.id,
      username: owner.username,
      name: owner.name,
      avatarUrl: owner.avatarUrl,
    },
    inviteCode: list.inviteCode,
    isOwner: currentUser.id === list.ownerId,
    itemsCount: state.items.get(list.id)?.length ?? 0,
    createdAt: list.createdAt,
    updatedAt: list.updatedAt,
  }
}

function buildItemResponse(item: MockItem) {
  const creator = state.users.get(item.createdBy) ?? demoUser
  return {
    id: item.id,
    name: item.name,
    checked: item.checked,
    quantity: item.quantity,
    dueDate: item.dueDate,
    url: item.url,
    position: item.position,
    createdBy: {
      id: creator.id,
      username: creator.username,
      name: creator.name,
      avatarUrl: creator.avatarUrl,
    },
    createdAt: item.createdAt,
    updatedAt: item.updatedAt,
  }
}

function json(res: Connect.ServerResponse, status: number, data: unknown) {
  res.statusCode = status
  res.setHeader('Content-Type', 'application/json')
  res.end(JSON.stringify(data))
}

function noContent(res: Connect.ServerResponse) {
  res.statusCode = 204
  res.end()
}

function notFound(res: Connect.ServerResponse) {
  json(res, 404, { detail: 'Mock route não encontrada.' })
}

async function readJsonBody(req: Connect.IncomingMessage) {
  const chunks: Buffer[] = []
  for await (const chunk of req) {
    chunks.push(Buffer.from(chunk))
  }

  if (chunks.length === 0) {
    return {}
  }

  return JSON.parse(Buffer.concat(chunks).toString('utf8'))
}

function toJoinListResponse(list: MockList) {
  const owner = state.users.get(list.ownerId) ?? demoUser
  return {
    id: list.id,
    name: list.name,
    type_slug: getType(list.typeId).slug,
    type_name: getType(list.typeId).name,
    owner_username: owner.username,
    owner_name: owner.name,
    owner_avatar_url: owner.avatarUrl,
    items: (state.items.get(list.id) ?? []).map((item) => ({
      id: item.id,
      name: item.name,
      checked: item.checked,
      quantity: item.quantity,
      due_date: item.dueDate,
      url: item.url,
      position: item.position,
    })),
    invite_code: list.inviteCode,
    expires_at: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
    mode: 'READ_ONLY' as const,
  }
}

export function createMockApiMiddleware(): Connect.NextHandleFunction {
  return async (req, res, next) => {
    if (!req.url?.startsWith('/api')) {
      next()
      return
    }

    const url = new URL(req.url, 'http://localhost:5173')
    const pathname = url.pathname
    const method = req.method ?? 'GET'
    const currentUser = getUserFromRequest(req)

    if (pathname === '/api/auth/login' && method === 'POST') {
      json(res, 200, {
        id: demoUser.id,
        username: demoUser.username,
        email: demoUser.email,
        name: demoUser.name,
        avatarUrl: demoUser.avatarUrl,
        onboardingCompletedAt: demoUser.onboardingCompletedAt,
        token: `mock-token-${demoUser.id}`,
      })
      return
    }

    if (pathname === '/api/auth/register' && method === 'POST') {
      const body = (await readJsonBody(req)) as {
        username?: string
        email?: string
        name?: string
      }
      const user: MockUser = {
        id: mockId('user'),
        username: body.username ?? `mock-${state.users.size + 1}`,
        email: body.email ?? `mock${state.users.size + 1}@test.com`,
        name: body.name ?? 'Nova Pessoa',
        avatarUrl: null,
        onboardingCompletedAt: null,
      }
      state.users.set(user.id, user)
      json(res, 201, {
        id: user.id,
        username: user.username,
        email: user.email,
        name: user.name,
        avatarUrl: user.avatarUrl,
        authProvider: 'EMAIL',
        createdAt: now(),
      })
      return
    }

    if (pathname === '/api/users/me' && method === 'GET') {
      json(res, 200, {
        id: currentUser.id,
        username: currentUser.username,
        email: currentUser.email,
        name: currentUser.name,
        avatarUrl: currentUser.avatarUrl,
        onboardingCompletedAt: currentUser.onboardingCompletedAt,
        authProvider: 'EMAIL',
      })
      return
    }

    if (pathname === '/api/users/me' && method === 'PATCH') {
      const body = (await readJsonBody(req)) as { name?: string; avatarUrl?: string }
      const nextUser = {
        ...currentUser,
        name: body.name?.trim() || currentUser.name,
        avatarUrl: body.avatarUrl ?? currentUser.avatarUrl,
      }
      state.users.set(nextUser.id, nextUser)
      json(res, 200, {
        username: nextUser.username,
        email: nextUser.email,
        name: nextUser.name,
        avatarUrl: nextUser.avatarUrl,
        authProvider: 'EMAIL',
        onboardingCompletedAt: nextUser.onboardingCompletedAt,
      })
      return
    }

    if (pathname === '/api/users/me/onboarding/complete' && method === 'POST') {
      const completedAt = now()
      state.users.set(currentUser.id, { ...currentUser, onboardingCompletedAt: completedAt })
      noContent(res)
      return
    }

    if (pathname === '/api/users/search' && method === 'GET') {
      const query = (url.searchParams.get('query') ?? '').toLowerCase()
      const results = [...state.users.values()]
        .filter((user) => user.username.toLowerCase().includes(query) || user.name.toLowerCase().includes(query))
        .map((user) => ({ username: user.username, name: user.name, avatarUrl: user.avatarUrl }))
      json(res, 200, results)
      return
    }

    if (pathname === '/api/lists' && method === 'GET') {
      const lists = [...state.lists.values()]
        .filter((list) => list.memberIds.includes(currentUser.id))
        .map((list) => buildListResponse(list, currentUser))
      json(res, 200, lists)
      return
    }

    if (pathname === '/api/lists' && method === 'POST') {
      const body = (await readJsonBody(req)) as { name?: string; typeId?: number }
      const createdAt = now()
      const newList: MockList = {
        id: mockId('list'),
        name: body.name?.trim() || 'Nova Lista',
        typeId: body.typeId ?? 4,
        ownerId: currentUser.id,
        inviteCode: `MOCK${Math.random().toString(36).slice(2, 8).toUpperCase()}`,
        createdAt,
        updatedAt: createdAt,
        revision: 1,
        memberIds: [currentUser.id],
      }
      state.lists.set(newList.id, newList)
      state.items.set(newList.id, [])
      state.activities.set(newList.id, [])
      json(res, 201, buildListResponse(newList, currentUser))
      return
    }

    const listMatch = pathname.match(/^\/api\/lists\/([^/]+)$/)
    if (listMatch) {
      const list = state.lists.get(listMatch[1])
      if (!list) {
        notFound(res)
        return
      }

      if (method === 'GET') {
        json(res, 200, buildListResponse(list, currentUser))
        return
      }

      if (method === 'PATCH') {
        const body = (await readJsonBody(req)) as { name?: string }
        list.name = body.name?.trim() || list.name
        list.updatedAt = now()
        list.revision += 1
        state.lists.set(list.id, list)
        json(res, 200, buildListResponse(list, currentUser))
        return
      }

      if (method === 'DELETE') {
        state.lists.delete(list.id)
        state.items.delete(list.id)
        state.activities.delete(list.id)
        noContent(res)
        return
      }
    }

    const stateMatch = pathname.match(/^\/api\/lists\/([^/]+)\/state$/)
    if (stateMatch) {
      const list = state.lists.get(stateMatch[1])
      if (!list) {
        notFound(res)
        return
      }
      json(res, 200, {
        listId: list.id,
        revision: list.revision,
        updatedAt: list.updatedAt,
        itemsCount: state.items.get(list.id)?.length ?? 0,
      })
      return
    }

    const itemsMatch = pathname.match(/^\/api\/lists\/([^/]+)\/items$/)
    if (itemsMatch) {
      const listId = itemsMatch[1]
      const items = state.items.get(listId) ?? []

      if (method === 'GET') {
        json(res, 200, items.map(buildItemResponse))
        return
      }

      if (method === 'POST') {
        const body = (await readJsonBody(req)) as {
          name?: string
          quantity?: number | null
          dueDate?: string | null
          url?: string | null
        }
        const nextItem: MockItem = {
          id: mockId('item'),
          listId,
          name: body.name?.trim() || 'Novo item',
          checked: false,
          quantity: body.quantity ?? null,
          dueDate: body.dueDate ?? null,
          url: body.url ?? null,
          position: items.length,
          createdBy: currentUser.id,
          createdAt: now(),
          updatedAt: now(),
        }
        items.push(nextItem)
        state.items.set(listId, items)
        json(res, 201, buildItemResponse(nextItem))
        return
      }
    }

    const itemMatch = pathname.match(/^\/api\/lists\/([^/]+)\/items\/([^/]+)$/)
    if (itemMatch) {
      const listId = itemMatch[1]
      const itemId = itemMatch[2]
      const items = state.items.get(listId) ?? []
      const item = items.find((candidate) => candidate.id === itemId)
      if (!item) {
        notFound(res)
        return
      }

      if (method === 'PATCH') {
        const body = (await readJsonBody(req)) as {
          name?: string
          quantity?: number | null
          dueDate?: string | null
          url?: string | null
        }
        item.name = body.name?.trim() || item.name
        item.quantity = body.quantity ?? item.quantity
        item.dueDate = body.dueDate ?? item.dueDate
        item.url = body.url ?? item.url
        item.updatedAt = now()
        json(res, 200, buildItemResponse(item))
        return
      }

      if (method === 'DELETE') {
        state.items.set(
          listId,
          items.filter((candidate) => candidate.id !== itemId)
        )
        noContent(res)
        return
      }
    }

    const toggleMatch = pathname.match(/^\/api\/lists\/([^/]+)\/items\/([^/]+)\/check$/)
    if (toggleMatch && method === 'PATCH') {
      const listId = toggleMatch[1]
      const itemId = toggleMatch[2]
      const items = state.items.get(listId) ?? []
      const item = items.find((candidate) => candidate.id === itemId)
      if (!item) {
        notFound(res)
        return
      }
      item.checked = !item.checked
      item.updatedAt = now()
      json(res, 200, buildItemResponse(item))
      return
    }

    const membersMatch = pathname.match(/^\/api\/lists\/([^/]+)\/members$/)
    if (membersMatch && method === 'GET') {
      const list = state.lists.get(membersMatch[1])
      if (!list) {
        notFound(res)
        return
      }
      json(
        res,
        200,
        list.memberIds.map((memberId) => {
          const user = state.users.get(memberId) ?? demoUser
          return {
            user: {
              id: user.id,
              username: user.username,
              name: user.name,
              avatar_url: user.avatarUrl,
            },
            role: memberId === list.ownerId ? 'OWNER' : 'MEMBER',
            joined_at: list.createdAt,
          }
        })
      )
      return
    }

    const memberDeleteMatch = pathname.match(/^\/api\/lists\/([^/]+)\/members\/([^/]+)$/)
    if (memberDeleteMatch && method === 'DELETE') {
      const listId = memberDeleteMatch[1]
      const userId = memberDeleteMatch[2]
      const list = state.lists.get(listId)
      if (!list) {
        notFound(res)
        return
      }
      list.memberIds = list.memberIds.filter((memberId) => memberId !== userId)
      state.lists.set(listId, list)
      noContent(res)
      return
    }

    const leaveMatch = pathname.match(/^\/api\/lists\/([^/]+)\/leave$/)
    if (leaveMatch && method === 'POST') {
      const list = state.lists.get(leaveMatch[1])
      if (!list) {
        notFound(res)
        return
      }
      list.memberIds = list.memberIds.filter((memberId) => memberId !== currentUser.id)
      state.lists.set(list.id, list)
      noContent(res)
      return
    }

    const inviteLinkMatch = pathname.match(/^\/api\/lists\/([^/]+)\/invite-link$/)
    if (inviteLinkMatch && method === 'POST') {
      const list = state.lists.get(inviteLinkMatch[1])
      if (!list) {
        notFound(res)
        return
      }
      json(res, 200, {
        invite_code: list.inviteCode,
        invite_link: `http://localhost:5173/join/${list.inviteCode}`,
        expires_at: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
      })
      return
    }

    const inviteByUsernameMatch = pathname.match(/^\/api\/lists\/([^/]+)\/invite$/)
    if (inviteByUsernameMatch && method === 'POST') {
      const listId = inviteByUsernameMatch[1]
      const list = state.lists.get(listId)
      if (!list) {
        notFound(res)
        return
      }
      const body = (await readJsonBody(req)) as { username?: string }
      const invitedUser =
        [...state.users.values()].find((user) => user.username === body.username) ??
        {
          id: mockId('user'),
          username: body.username ?? `guest-${state.users.size + 1}`,
          email: `${body.username ?? 'guest'}@test.com`,
          name: body.username ?? 'Convidado',
          avatarUrl: null,
          onboardingCompletedAt: null,
        }
      state.users.set(invitedUser.id, invitedUser)
      if (!list.memberIds.includes(invitedUser.id)) {
        list.memberIds.push(invitedUser.id)
      }
      state.lists.set(list.id, list)
      json(res, 200, {
        invited_username: invitedUser.username,
        message: `${invitedUser.username} adicionado!`,
      })
      return
    }

    const activityMatch = pathname.match(/^\/api\/lists\/([^/]+)\/activity$/)
    if (activityMatch && method === 'GET') {
      const activities = state.activities.get(activityMatch[1]) ?? []
      json(res, 200, {
        content: activities.map((activity) => ({
          id: activity.id,
          eventType: activity.type,
          actor: { id: demoUser.id, username: demoUser.username, name: activity.actorName, avatarUrl: null },
          itemName: activity.itemName ?? null,
          createdAt: activity.createdAt,
        })),
        totalElements: activities.length,
        totalPages: 1,
        size: activities.length || 20,
        number: 0,
      })
      return
    }

    const joinMatch = pathname.match(/^\/api\/lists\/join\/([^/]+)$/)
    if (joinMatch) {
      const list = [...state.lists.values()].find((candidate) => candidate.inviteCode === joinMatch[1])
      if (!list) {
        notFound(res)
        return
      }

      if (method === 'GET') {
        json(res, 200, toJoinListResponse(list))
        return
      }

      if (method === 'POST') {
        if (!list.memberIds.includes(currentUser.id)) {
          list.memberIds.push(currentUser.id)
        }
        state.lists.set(list.id, list)
        json(res, 201, {
          id: list.id,
          name: list.name,
          type_slug: getType(list.typeId).slug,
          type_name: getType(list.typeId).name,
          role: 'MEMBER',
          message: 'Voce entrou na lista.',
          created: true,
          list: buildListResponse(list, currentUser),
        })
        return
      }
    }

    notFound(res)
  }
}
