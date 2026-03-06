import 'axios'

declare module 'axios' {
  interface AxiosRequestConfig {
    preserveSessionOnUnauthorized?: boolean
  }

  interface InternalAxiosRequestConfig {
    preserveSessionOnUnauthorized?: boolean
  }
}
