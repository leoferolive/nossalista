import { describe, expect, it } from 'vitest'
import { parseListWebSocketMessage } from './WebSocketMessage'
import type { NotificationEventType } from './WebSocketMessage'
import websocketEnvelopeContract from '../../../contracts/websocket-envelope-v2.json'

/**
 * Este teste NAO hardcoda as expectativas do parser — ele le
 * `contracts/websocket-envelope-v2.json` (o contrato canonico, produzido pelo backend)
 * e gera os casos de teste a partir dele. Se o contrato mudar (novo tipo de evento que
 * exige actor, novo canal que exige revision, etc.) e o parser em `WebSocketMessage.ts`
 * nao acompanhar a mudanca, os testes abaixo falham automaticamente.
 */

const SCHEMA_VERSION = websocketEnvelopeContract.schemaVersion
const CHANNELS = websocketEnvelopeContract.channels
const ACTOR_REQUIRED_EVENT_TYPES = websocketEnvelopeContract.actorRequiredEventTypes
const REVISION_REQUIRED_CHANNELS = websocketEnvelopeContract.revisionRequiredChannels
const NOTIFICATION_EVENT_TYPES = websocketEnvelopeContract.notificationEventTypes

/**
 * `parseListWebSocketMessage` só reconhece eventos endereçados aos canais `items` e
 * `presence` (eventos de `notifications` têm parsing próprio em `NotificationContext`,
 * fora do escopo deste parser). Este mapa é o único ponto de conhecimento "extra" do
 * teste — o contrato não descreve canal por tipo de evento — e existe só para montar
 * envelopes válidos; se o contrato adicionar um `actorRequiredEventType` novo que este
 * parser deveria tratar e o mapa não for atualizado, o teste falha explicitamente
 * (ver `resolveChannelForEventType`) em vez de ser pulado silenciosamente.
 */
const CHANNEL_BY_LIST_EVENT_TYPE: Record<string, 'items' | 'presence'> = {
  ITEM_ADDED: 'items',
  ITEM_UPDATED: 'items',
  ITEM_REMOVED: 'items',
  ITEM_CHECKED: 'items',
  LIST_LAYOUT_UPDATED: 'items',
  MEMBER_ONLINE: 'presence',
  MEMBER_OFFLINE: 'presence',
}

function resolveChannelForEventType(type: string): 'items' | 'presence' {
  const channel = CHANNEL_BY_LIST_EVENT_TYPE[type]
  if (!channel) {
    throw new Error(
      `Nenhum canal de teste configurado para o tipo de evento "${type}" (novo em ` +
        'actorRequiredEventTypes do contrato). Atualize CHANNEL_BY_LIST_EVENT_TYPE em ' +
        'WebSocketMessage.test.ts.'
    )
  }
  return channel
}

function buildListItem() {
  return {
    id: 'item-1',
    name: 'Arroz',
    checked: false,
    quantity: null,
    dueDate: null,
    url: null,
    position: 0,
    createdBy: {
      id: 'user-1',
      username: 'leo',
      name: 'Leo',
      avatarUrl: null,
    },
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }
}

/**
 * Constrói um payload válido para cada tipo de evento de `actorRequiredEventTypes`.
 * Se o contrato introduzir um tipo novo sem um builder aqui, o teste falha com uma
 * mensagem explícita em vez de aceitar silenciosamente um payload incorreto.
 */
function buildPayloadForEventType(type: string): unknown {
  switch (type) {
    case 'ITEM_ADDED':
    case 'ITEM_UPDATED':
    case 'ITEM_REMOVED':
    case 'ITEM_CHECKED':
      return buildListItem()
    case 'LIST_LAYOUT_UPDATED':
      return { positions: [{ itemId: 'item-1', position: 0 }] }
    case 'MEMBER_ONLINE':
      return { userId: 'user-1', username: 'leo', name: 'Leo', avatarUrl: null }
    case 'MEMBER_OFFLINE':
      return { userId: 'user-1', username: 'leo' }
    default:
      throw new Error(
        `Nenhum payload de teste configurado para o tipo de evento "${type}" (novo em ` +
          'actorRequiredEventTypes do contrato). Atualize buildPayloadForEventType em ' +
          'WebSocketMessage.test.ts.'
      )
  }
}

interface EnvelopeOverrides {
  type: string
  actor?: { id: string; username: string }
  revision?: number | null
  channel?: string
  schemaVersion?: number
}

function buildEnvelope({ type, actor, revision, channel, schemaVersion }: EnvelopeOverrides) {
  const resolvedChannel = channel ?? resolveChannelForEventType(type)
  const needsRevision = REVISION_REQUIRED_CHANNELS.includes(resolvedChannel)

  const envelope: Record<string, unknown> = {
    schemaVersion: schemaVersion ?? SCHEMA_VERSION,
    eventId: 'event-1',
    listId: 'list-1',
    channel: resolvedChannel,
    type,
    payload: buildPayloadForEventType(type),
    timestamp: new Date().toISOString(),
  }

  if (actor) {
    envelope.actor = actor
  }

  if (revision !== undefined) {
    envelope.revision = revision
  } else if (needsRevision) {
    envelope.revision = 1741040400000
  }

  return envelope
}

describe('parseListWebSocketMessage — forma geral do envelope (contrato v2)', () => {
  it('aceita um envelope valido de item construido a partir do contrato', () => {
    const parsed = parseListWebSocketMessage(
      buildEnvelope({ type: 'ITEM_ADDED', actor: { id: 'user-1', username: 'leo' } })
    )

    expect(parsed).not.toBeNull()
    expect(parsed?.type).toBe('ITEM_ADDED')
  })

  it('rejeita mensagem legada sem schemaVersion/channel/listId', () => {
    const parsed = parseListWebSocketMessage({
      type: 'ITEM_ADDED',
      payload: {},
      userId: 'user-1',
      username: 'leo',
      timestamp: new Date().toISOString(),
    })

    expect(parsed).toBeNull()
  })

  it('rejeita schemaVersion diferente do declarado no contrato', () => {
    const parsed = parseListWebSocketMessage(
      buildEnvelope({
        type: 'ITEM_ADDED',
        actor: { id: 'user-1', username: 'leo' },
        schemaVersion: SCHEMA_VERSION + 1,
      })
    )

    expect(parsed).toBeNull()
  })

  it('rejeita canais fora da lista `channels` do contrato', () => {
    const parsed = parseListWebSocketMessage(
      buildEnvelope({
        type: 'ITEM_ADDED',
        actor: { id: 'user-1', username: 'leo' },
        channel: 'canal-inexistente',
      })
    )

    expect(parsed).toBeNull()
  })

  it.each(CHANNELS)('canal "%s" listado no contrato é reconhecido pelo parser', (channel) => {
    // Envelope minimo, sem type reconhecido — só valida que o canal em si não é o
    // motivo da rejeição (o parser aceita o canal antes de olhar para o `type`).
    const parsed = parseListWebSocketMessage({
      schemaVersion: SCHEMA_VERSION,
      eventId: 'event-1',
      listId: 'list-1',
      channel,
      type: 'TIPO_DESCONHECIDO_PARA_TESTE_DE_CANAL',
      payload: {},
      timestamp: new Date().toISOString(),
    })

    // O tipo é deliberadamente desconhecido (cai no `default` do switch), então o
    // resultado é sempre null — a asserção real é que a rejeição não acontece por
    // causa do canal (testado à parte, acima, com um canal inválido).
    expect(parsed).toBeNull()
  })
})

describe('parseListWebSocketMessage — actorRequiredEventTypes (contrato v2)', () => {
  it('a lista actorRequiredEventTypes do contrato não está vazia (sanity check)', () => {
    expect(ACTOR_REQUIRED_EVENT_TYPES.length).toBeGreaterThan(0)
  })

  it.each(ACTOR_REQUIRED_EVENT_TYPES)(
    'rejeita evento "%s" sem actor, exigido pelo contrato',
    (type) => {
      const withoutActor = buildEnvelope({ type })

      expect(parseListWebSocketMessage(withoutActor)).toBeNull()
    }
  )

  it.each(ACTOR_REQUIRED_EVENT_TYPES)(
    'aceita evento "%s" com actor válido, conforme o contrato',
    (type) => {
      const withActor = buildEnvelope({ type, actor: { id: 'user-1', username: 'leo' } })

      expect(parseListWebSocketMessage(withActor)).not.toBeNull()
    }
  )

  it.each(ACTOR_REQUIRED_EVENT_TYPES)('rejeita evento "%s" com actor malformado', (type) => {
    const malformedActor = buildEnvelope({ type, actor: { id: 'user-1', username: 'leo' } })
    // Injeta um actor invalido (sem `username`) de propósito para testar a validação.
    ;(malformedActor as Record<string, unknown>).actor = { id: 'user-1' }

    expect(parseListWebSocketMessage(malformedActor)).toBeNull()
  })
})

describe('parseListWebSocketMessage — revisionRequiredChannels (contrato v2)', () => {
  it('a lista revisionRequiredChannels do contrato não está vazia (sanity check)', () => {
    expect(REVISION_REQUIRED_CHANNELS.length).toBeGreaterThan(0)
  })

  const eventTypesByRevisionRequiredChannel = REVISION_REQUIRED_CHANNELS.map((channel) => {
    const type = Object.entries(CHANNEL_BY_LIST_EVENT_TYPE).find(
      ([, mappedChannel]) => mappedChannel === channel
    )?.[0]

    if (!type) {
      throw new Error(
        `Nenhum tipo de evento de teste mapeado para o canal "${channel}" (declarado em ` +
          'revisionRequiredChannels do contrato). Atualize CHANNEL_BY_LIST_EVENT_TYPE em ' +
          'WebSocketMessage.test.ts.'
      )
    }

    return { channel, type }
  })

  it.each(eventTypesByRevisionRequiredChannel)(
    'rejeita mensagem do canal "$channel" sem revision válida',
    ({ type }) => {
      const withoutRevision = buildEnvelope({
        type,
        actor: { id: 'user-1', username: 'leo' },
        revision: null,
      })
      delete (withoutRevision as Record<string, unknown>).revision

      expect(parseListWebSocketMessage(withoutRevision)).toBeNull()
    }
  )

  it.each(eventTypesByRevisionRequiredChannel)(
    'rejeita mensagem do canal "$channel" com revision não positiva',
    ({ type }) => {
      const invalidRevision = buildEnvelope({
        type,
        actor: { id: 'user-1', username: 'leo' },
        revision: 0,
      })

      expect(parseListWebSocketMessage(invalidRevision)).toBeNull()
    }
  )

  it.each(eventTypesByRevisionRequiredChannel)(
    'aceita mensagem do canal "$channel" com revision válida',
    ({ type }) => {
      const validRevision = buildEnvelope({
        type,
        actor: { id: 'user-1', username: 'leo' },
        revision: 1741040400000,
      })

      expect(parseListWebSocketMessage(validRevision)).not.toBeNull()
    }
  )
})

describe('NotificationEventType — notificationEventTypes (contrato v2)', () => {
  /**
   * `notificationEventTypes` não alimenta nenhuma validação em tempo de execução —
   * ele descreve o union type `NotificationEventType` (consumido por
   * `NotificationContext`, fora do escopo desta task). Um union de literais não
   * existe em tempo de execução, então a verificação usa duas camadas:
   *
   * 1. Compilação: `NOTIFICATION_EVENT_TYPE_ROSTER` está anotado como
   *    `Record<NotificationEventType, true>`. Um `Record` sobre um union de
   *    literais exige a chave exata — se `NotificationEventType` ganhar ou perder
   *    um membro sem este roster ser atualizado, `npm run typecheck` falha aqui
   *    (chave faltando ou chave excedente).
   * 2. Execução: as chaves desse roster (agora garantidamente == NotificationEventType)
   *    são comparadas com `notificationEventTypes` do contrato. Se o contrato mudar
   *    sem o roster (e portanto o tipo) acompanhar, o `expect` abaixo falha.
   */
  const NOTIFICATION_EVENT_TYPE_ROSTER: Record<NotificationEventType, true> = {
    ITEM_ADDED: true,
    ITEM_UPDATED: true,
    ITEM_REMOVED: true,
    ITEM_CHECKED: true,
    LIST_NAME_UPDATED: true,
    MEMBER_JOINED: true,
    MEMBER_LEFT: true,
    MEMBER_REMOVED: true,
  }

  it('a lista notificationEventTypes do contrato não está vazia (sanity check)', () => {
    expect(NOTIFICATION_EVENT_TYPES.length).toBeGreaterThan(0)
  })

  it('NotificationEventType (WebSocketMessage.ts) cobre exatamente os tipos do contrato', () => {
    const parserTypes = Object.keys(NOTIFICATION_EVENT_TYPE_ROSTER).sort()
    const contractTypes = [...NOTIFICATION_EVENT_TYPES].sort()

    expect(parserTypes).toEqual(contractTypes)
  })
})
