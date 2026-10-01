const apiBase = process.env.NUXT_PUBLIC_API_BASE
  || (
    process.env.CODESPACE_NAME && process.env.GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN
      ? `https://${process.env.CODESPACE_NAME}-8080.${process.env.GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN}`
      : 'http://localhost:8080'
  )

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',

  future: {
    compatibilityVersion: 4
  },

  devtools: { enabled: false },

  devServer: {
    host: '0.0.0.0',
    port: 3000
  },

  modules: ['@nuxtjs/tailwindcss'],

  components: [
    {
      path: '~/components',
      pathPrefix: false
    }
  ],

  runtimeConfig: {
    public: {
      apiBase
    }
  }
})