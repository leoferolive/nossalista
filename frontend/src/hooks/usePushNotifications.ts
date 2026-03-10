import { useCallback, useState } from 'react'
import { pushApi } from '../api/pushApi'

function urlBase64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = atob(base64)
  const buffer = new ArrayBuffer(rawData.length)
  const view = new Uint8Array(buffer)
  for (let i = 0; i < rawData.length; i++) {
    view[i] = rawData.charCodeAt(i)
  }
  return view
}

export type PushPermissionState = 'default' | 'granted' | 'denied' | 'unsupported'

export function usePushNotifications() {
  const [permissionState, setPermissionState] = useState<PushPermissionState>(() => {
    if (!('Notification' in window) || !('serviceWorker' in navigator)) {
      return 'unsupported'
    }
    return Notification.permission as PushPermissionState
  })

  const requestPermission = useCallback(async () => {
    if (!('Notification' in window) || !('serviceWorker' in navigator)) {
      setPermissionState('unsupported')
      return
    }

    const permission = await Notification.requestPermission()
    setPermissionState(permission as PushPermissionState)

    if (permission !== 'granted') return

    try {
      const vapidPublicKey = await pushApi.getVapidPublicKey()
      if (!vapidPublicKey) return

      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidPublicKey),
      })

      await pushApi.subscribe(subscription.toJSON())
    } catch (err) {
      console.error('[Push] Falha ao registrar subscrição push:', err)
    }
  }, [])

  const unsubscribe = useCallback(async () => {
    try {
      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.getSubscription()
      if (!subscription) return

      await pushApi.unsubscribe(subscription.endpoint)
      await subscription.unsubscribe()
    } catch (err) {
      console.error('[Push] Falha ao cancelar subscrição push:', err)
    }
  }, [])

  return { permissionState, requestPermission, unsubscribe }
}
