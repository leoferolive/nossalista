import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { usePushNotifications } from './usePushNotifications'
import * as pushApiModule from '../api/pushApi'

vi.mock('../api/pushApi', () => ({
  pushApi: {
    getVapidPublicKey: vi.fn(),
    subscribe: vi.fn(),
    unsubscribe: vi.fn(),
  },
}))

const mockPushApi = vi.mocked(pushApiModule.pushApi)

function makeSubscription(endpoint = 'https://push.example.com/sub') {
  return {
    endpoint,
    unsubscribe: vi.fn().mockResolvedValue(true),
    toJSON: vi.fn().mockReturnValue({ endpoint, keys: { p256dh: 'abc', auth: 'def' } }),
  }
}

function setupServiceWorker(subscription: ReturnType<typeof makeSubscription> | null = null) {
  const pushManager = {
    subscribe: vi.fn().mockResolvedValue(makeSubscription()),
    getSubscription: vi.fn().mockResolvedValue(subscription),
  }
  const registration = { pushManager }
  Object.defineProperty(navigator, 'serviceWorker', {
    value: { ready: Promise.resolve(registration) },
    configurable: true,
  })
  return pushManager
}

describe('usePushNotifications', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    Object.defineProperty(window, 'Notification', {
      value: { permission: 'default', requestPermission: vi.fn() },
      configurable: true,
      writable: true,
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('deve retornar "default" como estado inicial quando APIs estão disponíveis', () => {
    setupServiceWorker()
    const { result } = renderHook(() => usePushNotifications())
    expect(result.current.permissionState).toBe('default')
  })

  it('deve retornar "unsupported" quando Notification API não está disponível', () => {
    const orig = window.Notification
    // @ts-expect-error testing absence of API
    delete window.Notification
    const { result } = renderHook(() => usePushNotifications())
    expect(result.current.permissionState).toBe('unsupported')
    window.Notification = orig
  })

  it('deve setar unsupported quando requestPermission chamado sem APIs', async () => {
    const orig = window.Notification
    // @ts-expect-error testing absence of API
    delete window.Notification
    const { result } = renderHook(() => usePushNotifications())

    await act(async () => {
      await result.current.enablePush()
    })

    expect(result.current.permissionState).toBe('unsupported')
    window.Notification = orig
  })

  it('deve setar "denied" quando permissão é negada', async () => {
    setupServiceWorker()
    window.Notification.requestPermission = vi.fn().mockResolvedValue('denied')

    const { result } = renderHook(() => usePushNotifications())

    await act(async () => {
      await result.current.enablePush()
    })

    expect(result.current.permissionState).toBe('denied')
    expect(mockPushApi.subscribe).not.toHaveBeenCalled()
  })

  it('deve subscrever ao push quando permissão concedida', async () => {
    const pushManager = setupServiceWorker()
    window.Notification.requestPermission = vi.fn().mockResolvedValue('granted')
    mockPushApi.getVapidPublicKey.mockResolvedValue(
      'BNbronVSVNFVUXJiMU1PQ1FNQkJOYnJvblZTVk5GVlVYSmk='
    )
    mockPushApi.subscribe.mockResolvedValue(undefined)

    const { result } = renderHook(() => usePushNotifications())

    await act(async () => {
      await result.current.enablePush()
    })

    expect(result.current.permissionState).toBe('granted')
    expect(result.current.pushEnabled).toBe(true)
    expect(pushManager.subscribe).toHaveBeenCalledWith(
      expect.objectContaining({ userVisibleOnly: true })
    )
    expect(mockPushApi.subscribe).toHaveBeenCalled()
  })

  it('deve abortar subscrição quando VAPID key não está disponível', async () => {
    const pushManager = setupServiceWorker()
    window.Notification.requestPermission = vi.fn().mockResolvedValue('granted')
    mockPushApi.getVapidPublicKey.mockResolvedValue(null)

    const { result } = renderHook(() => usePushNotifications())

    await act(async () => {
      await result.current.enablePush()
    })

    expect(pushManager.subscribe).not.toHaveBeenCalled()
  })

  it('deve cancelar subscrição existente com disablePush', async () => {
    const sub = makeSubscription()
    setupServiceWorker(sub)
    mockPushApi.unsubscribe.mockResolvedValue(undefined)

    const { result } = renderHook(() => usePushNotifications())

    await act(async () => {
      await result.current.disablePush()
    })

    expect(mockPushApi.unsubscribe).toHaveBeenCalledWith(sub.endpoint)
    expect(sub.unsubscribe).toHaveBeenCalled()
    expect(result.current.pushEnabled).toBe(false)
  })

  it('não deve falhar quando não há subscrição ativa para cancelar', async () => {
    setupServiceWorker(null)

    const { result } = renderHook(() => usePushNotifications())

    await act(async () => {
      await result.current.disablePush()
    })

    expect(mockPushApi.unsubscribe).not.toHaveBeenCalled()
  })

  it('deve expor pushEnabled true quando já existe subscrição ativa', async () => {
    const sub = makeSubscription()
    setupServiceWorker(sub)

    const { result } = renderHook(() => usePushNotifications())

    await act(async () => {
      await Promise.resolve()
    })

    expect(result.current.pushEnabled).toBe(true)
  })
})
