import axios, { type InternalAxiosRequestConfig } from 'axios'
import type { RespostaLogin } from '~/composables/useAuth'

/**
 * Cliente HTTP (axios) apontando para a API do back-end.
 * A baseURL vem de runtimeConfig.public.apiBase (nuxt.config.ts),
 * configurável via NUXT_PUBLIC_API_BASE (ex.: [env] no mise.local.toml).
 * O token do usuário logado (useAuth) vai em todas as requisições, exceto login e cadastro.
 *
 * Uso em qualquer componente/página:
 *   const { $api } = useNuxtApp()
 *   const { data } = await $api.get('/algum-endpoint')
 */

// o back-end responde 401 a um token inválido ou expirado mesmo nestas rotas, então elas nunca levam o token
const ROTAS_PUBLICAS = ['/api/auth/login', '/api/usuarios']

function ehRotaPublica(config: InternalAxiosRequestConfig) {
  return config.method === 'post' && ROTAS_PUBLICAS.includes(config.url ?? '')
}

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const { token, definirSessao } = useAuth()

  const api = axios.create({
    baseURL: config.public.apiBase,
    timeout: 5000
  })

  // Login automático com o usuário de teste: só no `nuxt dev` e com LOGIN_AUTOMATICO=true (veja o README).
  const { email, senha } = config.public.loginAutomatico
  const loginAutomaticoAtivo = import.meta.dev && Boolean(email && senha)
  let loginEmAndamento: Promise<void> | null = null

  function garantirLoginAutomatico() {
    // várias requisições sem token ao mesmo tempo esperam o mesmo login
    loginEmAndamento ??= api.post<RespostaLogin>('/api/auth/login', { email, senha })
      .then(({ data }) => definirSessao(data))
      .finally(() => { loginEmAndamento = null })
    return loginEmAndamento
  }

  api.interceptors.request.use(async (requisicao) => {
    if (ehRotaPublica(requisicao)) {
      return requisicao
    }
    if (!token.value && loginAutomaticoAtivo) {
      await garantirLoginAutomatico()
    }
    if (token.value) {
      requisicao.headers.Authorization = `Bearer ${token.value}`
    }
    return requisicao
  })

  // com o login automático, um token expirado (401) é renovado e a requisição é repetida uma vez
  api.interceptors.response.use(undefined, async (erro) => {
    const requisicao = erro.config as (InternalAxiosRequestConfig & { _repetida?: boolean }) | undefined
    if (
      loginAutomaticoAtivo
      && erro.response?.status === 401
      && requisicao
      && !requisicao._repetida
      && !ehRotaPublica(requisicao)
    ) {
      requisicao._repetida = true
      token.value = null
      await garantirLoginAutomatico()
      return api(requisicao)
    }
    return Promise.reject(erro)
  })

  return {
    provide: {
      api
    }
  }
})
