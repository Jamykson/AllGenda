// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  modules: ['@nuxtjs/tailwindcss'],

  runtimeConfig: {
    public: {
      // URL base da API do back-end (Spring Boot). Sobrescrevível via
      // variável de ambiente NUXT_PUBLIC_API_BASE (ver .env.example).
      apiBase: 'http://localhost:8080'
    }
  }
})