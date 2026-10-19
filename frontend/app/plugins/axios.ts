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

export default defineNuxtPlugin((nuxtApp) => {
  const config = useRuntimeConfig()
  const { token, definirSessao, sair } = useAuth()

  const api = axios.create({
    baseURL: config.public.apiBase,
    timeout: 5000
  })

  // Usuário de teste nas chamadas à API: só no `nuxt dev` e com API_COM_USUARIO_DE_TESTE=true (veja o README).
  // Não muda nenhuma tela; sem token, as requisições usam um token obtido com o usuário de teste.
  const { email, senha } = config.public.usuarioDeTesteNaApi
  const usuarioDeTesteAtivo = import.meta.dev && Boolean(email && senha)
  let loginEmAndamento: Promise<void> | null = null

  function garantirTokenDeTeste() {
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
    if (!token.value && usuarioDeTesteAtivo) {
      await garantirTokenDeTeste()
    }
    if (token.value) {
      requisicao.headers.Authorization = `Bearer ${token.value}`
    }
    return requisicao
  })

  // Um 401 em uma rota protegida encerra a sessão, inclusive no modo de teste.
  // Credenciais inválidas no login ficam no formulário e não causam redirecionamento.
  api.interceptors.response.use(undefined, async (erro) => {
    const requisicao = erro.config as InternalAxiosRequestConfig | undefined
    if (erro.response?.status === 401 && requisicao && !ehRotaPublica(requisicao)) {
      sair()
      await nuxtApp.runWithContext(() => navigateTo({ path: '/', query: { sessao: 'expirada' } }))
    }
    return Promise.reject(erro)
  })

  return {
    provide: {
      api
    }
  }
})
