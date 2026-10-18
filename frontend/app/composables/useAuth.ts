export interface UsuarioLogado {
  id: string
  nome: string
  email: string
}

export interface RespostaLogin {
  token: string
  tipo: string
  expiraEm: number
  usuario: UsuarioLogado
}

const CHAVE_TOKEN = 'allgenda-token'
const CHAVE_USUARIO = 'allgenda-usuario'

/**
 * Sessão do usuário logado: token JWT e dados do usuário.
 * O estado é compartilhado entre componentes (useState) e guardado em cookie, que dura o mesmo
 * que o token (1 hora) e também pode ser lido no SSR.
 *
 * Uso:
 *   const { usuario, entrar, sair } = useAuth()
 *   await entrar('maria@example.com', 'senha-forte-123')
 */
export function useAuth() {
  const opcoesCookie = { maxAge: 3600, sameSite: 'lax' as const }
  const cookieToken = useCookie<string | null>(CHAVE_TOKEN, opcoesCookie)
  const cookieUsuario = useCookie<UsuarioLogado | null>(CHAVE_USUARIO, opcoesCookie)

  const token = useState<string | null>('auth-token', () => cookieToken.value ?? null)
  const usuario = useState<UsuarioLogado | null>('auth-usuario', () => cookieUsuario.value ?? null)

  function definirSessao(resposta: RespostaLogin) {
    token.value = resposta.token
    usuario.value = resposta.usuario
    cookieToken.value = resposta.token
    cookieUsuario.value = resposta.usuario
  }

  async function entrar(email: string, senha: string): Promise<UsuarioLogado> {
    const { $api } = useNuxtApp()
    const { data } = await $api.post<RespostaLogin>('/api/auth/login', { email, senha })
    definirSessao(data)
    return data.usuario
  }

  function sair() {
    token.value = null
    usuario.value = null
    cookieToken.value = null
    cookieUsuario.value = null
  }

  return {
    token,
    usuario,
    definirSessao,
    entrar,
    sair
  }
}
