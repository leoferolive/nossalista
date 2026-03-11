import { useCallback, useEffect, useState } from 'react'
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
export type PushAvailability = 'available' | 'unsupported' | 'server-not-configured' | 'checking'

export function usePushNotifications() {
  const [permissionState, setPermissionState] = useState<PushPermissionState>(() => {
    if (!('Notification' in window) || !('serviceWorker' in navigator)) {
      return 'unsupported'
    }
    return Notification.permission as PushPermissionState
  })
  const [pushEnabled, setPushEnabled] = useState<boolean>(false)
  const [availability, setAvailability] = useState<PushAvailability>(() => {
    if (!('Notification' in window) || !('serviceWorker' in navigator)) {
      return 'unsupported'
    }
    return 'checking'
  })

  useEffect(() => {
    if (!('serviceWorker' in navigator)) {
      setPushEnabled(false)
      setAvailability('unsupported')
      return
    }

    let cancelled = false
    ;(async () => {
      try {
        const vapidPublicKey = await pushApi.getVapidPublicKey()
        if (!vapidPublicKey) {
          if (!cancelled) {
            setAvailability('server-not-configured')
            setPushEnabled(false)
          }
          return
        }

        const registration = await navigator.serviceWorker.ready
        const subscription = await registration.pushManager.getSubscription()
        if (!cancelled) {
          setAvailability('available')
          setPushEnabled(Boolean(subscription))
        }
      } catch {
        if (!cancelled) {
          setAvailability('server-not-configured')
          setPushEnabled(false)
        }
      }
    })()

    return () => {
      cancelled = true
    }
  }, [])

  const enablePush = useCallback(async () => {
    if (!('Notification' in window) || !('serviceWorker' in navigator)) {
      setPermissionState('unsupported')
      setAvailability('unsupported')
      return
    }

    let permission = Notification.permission
    if (permission !== 'granted') {
      permission = await Notification.requestPermission()
      setPermissionState(permission as PushPermissionState)
    }

    if (permission !== 'granted') return

    try {
      const vapidPublicKey = await pushApi.getVapidPublicKey()
      if (!vapidPublicKey) {
        setAvailability('server-not-configured')
        return
      }

      const registration = await navigator.serviceWorker.ready
      let subscription = await registration.pushManager.getSubscription()

      if (!subscription) {
        subscription = await registration.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: urlBase64ToUint8Array(vapidPublicKey),
        })
      }

      await pushApi.subscribe(subscription.toJSON())
      setAvailability('available')
      setPushEnabled(true)
    } catch (err) {
      console.error('[Push] Falha ao registrar subscrição push:', err)
    }
  }, [])

  const disablePush = useCallback(async () => {
    try {
      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.getSubscription()
      if (!subscription) {
        setPushEnabled(false)
        return
      }

      await pushApi.unsubscribe(subscription.endpoint)
      await subscription.unsubscribe()
      setPushEnabled(false)
    } catch (err) {
      console.error('[Push] Falha ao cancelar subscrição push:', err)
    }
  }, [])

  return { permissionState, pushEnabled, availability, enablePush, disablePush }
}
