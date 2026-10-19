<script setup lang="ts">
// Mostra uma imagem já salva, de uma anotação ou avulsa na aula. Ela é baixada com o token
// (o <img src> direto não o enviaria) e exibida por uma URL temporária, liberada quando o componente sai da tela.

const props = defineProps<{
  imagem: { url: string, nome: string }
}>()

const { baixarImagem } = useAnotacoes()

const src = ref<string | null>(null)
const falhou = ref(false)

onMounted(async () => {
  try {
    const conteudo = await baixarImagem(props.imagem)
    src.value = URL.createObjectURL(conteudo)
  }
  catch (erro) {
    console.error('[anotacao] erro ao baixar imagem:', erro)
    falhou.value = true
  }
})

onBeforeUnmount(() => {
  if (src.value) {
    URL.revokeObjectURL(src.value)
  }
})
</script>

<template>
  <img v-if="src" :src="src" :alt="imagem.nome" />
  <span v-else-if="falhou" class="imagem-indisponivel" role="img" :aria-label="`Imagem ${imagem.nome} indisponível`">
    Não foi possível carregar a imagem.
  </span>
  <span v-else class="imagem-carregando" role="status">Carregando imagem...</span>
</template>

<style scoped>
.imagem-indisponivel,
.imagem-carregando {
  display: grid;
  min-height: 120px;
  place-items: center;
  border-radius: 8px;
  background: #e2e8f0;
  color: #64748b;
  font-size: 12px;
}
</style>
