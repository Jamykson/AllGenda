export default defineNuxtRouteMiddleware((to) => {
  if (['/', '/cadastro', '/esqueci-senha'].includes(to.path)) return
  const { token } = useAuth()
  // O modo de desenvolvimento existente pode obter seu primeiro token na chamada à API.
  const teste = useRuntimeConfig().public.usuarioDeTesteNaApi
  const modoDeTeste = import.meta.dev && Boolean(teste.email && teste.senha)
  if (!token.value && !modoDeTeste) {
    return navigateTo({ path: '/', query: { retorno: to.fullPath } })
  }
})
