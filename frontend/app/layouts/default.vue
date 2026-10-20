<script setup lang="ts">
const route = useRoute()

// a agenda e a página da aula ocupam toda a área, sem margem, e cuidam da própria rolagem
const telaCheia = computed(() => route.path === '/agenda' || route.path.startsWith('/aulas/'))
</script>

<template>
  <div class="app">
    <Sidebar />

    <main
      class="conteudo"
      :class="{ 'conteudo-agenda': telaCheia }"
    >
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app {
  display: flex;
  width: 100%;
  min-height: 100vh;
  background: #f7f8fa;
}

.conteudo {
  flex: 1;
  min-width: 0;
  padding: 24px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  box-sizing: border-box;
}

.conteudo-agenda {
  padding: 0;
  justify-content: flex-start;
  align-items: stretch;
  overflow: hidden;
}

@media (max-width: 768px) {
  .app {
    flex-direction: column;
  }

  .conteudo {
    padding: 16px;
  }

  .conteudo.conteudo-agenda {
    padding: 0;
  }
}
</style>