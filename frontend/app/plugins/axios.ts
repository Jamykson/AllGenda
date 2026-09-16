import axios from 'axios'

/**
 * Cliente HTTP (axios) apontando para a API do back-end.
 * A baseURL vem de runtimeConfig.public.apiBase (nuxt.config.ts),
 * configurável via NUXT_PUBLIC_API_BASE no .env.
 *
 * Uso em qualquer componente/página:
 *   const { $api } = useNuxtApp()
 *   const { data } = await $api.get('/algum-endpoint')
 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()

  const api = axios.create({
    baseURL: config.public.apiBase,
    timeout: 5000
  })

  return {
    provide: {
      api
    }
  }
})