<script setup lang="ts">
import { ChevronLeft, ChevronRight, PencilLine, Trash2, X } from '@lucide/vue'
import type { ImagemAula } from '~/types/anotacao'

// Imagem da galeria em tamanho grande, com a legenda. As setas passam para a imagem anterior ou
// seguinte e Esc fecha. O autor edita a legenda e exclui a imagem por aqui.

const props = defineProps<{
  imagens: ImagemAula[]
  indice: number
}>()

const emit = defineEmits<{
  fechar: []
  'update:indice': [indice: number]
  salva: [imagem: ImagemAula]
  excluida: [imagemId: string]
}>()

const { editarLegenda, removerImagemAula } = useAnotacoes()
const { usuario } = useAuth()

const editando = ref(false)
const legendaEditada = ref('')
const ocupado = ref(false)
const erro = ref('')
const botaoFechar = ref<HTMLButtonElement | null>(null)
const campoLegenda = ref<HTMLInputElement | null>(null)
let focoAnterior: HTMLElement | null = null

const imagem = computed(() => props.imagens[props.indice])
const ehDoUsuario = computed(() => Boolean(usuario.value) && imagem.value?.autorId === usuario.value?.id)

function irPara(indice: number) {
  if (indice < 0 || indice >= props.imagens.length) return
  editando.value = false
  erro.value = ''
  emit('update:indice', indice)
}

function aoPressionarTecla(evento: KeyboardEvent) {
  // dentro do campo da legenda, as setas movem o cursor e Esc só cancela a edição
  if (editando.value) return
  if (evento.key === 'Escape') emit('fechar')
  else if (evento.key === 'ArrowLeft') irPara(props.indice - 1)
  else if (evento.key === 'ArrowRight') irPara(props.indice + 1)
}

async function iniciarEdicao() {
  if (!imagem.value) return
  legendaEditada.value = imagem.value.legenda ?? ''
  erro.value = ''
  editando.value = true
  await nextTick()
  campoLegenda.value?.focus()
}

async function salvarLegenda() {
  if (!imagem.value || ocupado.value) return
  const legenda = legendaEditada.value.trim()
  if (legenda === (imagem.value.legenda ?? '')) {
    editando.value = false
    return
  }

  ocupado.value = true
  erro.value = ''
  try {
    emit('salva', await editarLegenda(imagem.value, legenda))
    editando.value = false
  }
  catch (falha) {
    console.error('[anotacao] erro ao editar legenda:', falha)
    erro.value = mensagemDeErro(falha, 'Não foi possível salvar a legenda.')
  }
  finally {
    ocupado.value = false
  }
}

async function excluirImagem() {
  if (!imagem.value || ocupado.value || !window.confirm('Deseja excluir esta imagem?')) return

  ocupado.value = true
  erro.value = ''
  try {
    await removerImagemAula(imagem.value)
    emit('excluida', imagem.value.id)
  }
  catch (falha) {
    console.error('[anotacao] erro ao excluir imagem avulsa:', falha)
    erro.value = mensagemDeErro(falha, 'Não foi possível excluir a imagem.')
  }
  finally {
    ocupado.value = false
  }
}

onMounted(() => {
  focoAnterior = document.activeElement instanceof HTMLElement ? document.activeElement : null
  botaoFechar.value?.focus()
  document.addEventListener('keydown', aoPressionarTecla)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', aoPressionarTecla)
  focoAnterior?.focus()
})
</script>

<template>
  <div
    v-if="imagem"
    class="visualizador"
    role="dialog"
    aria-modal="true"
    :aria-label="`Imagem ${indice + 1} de ${imagens.length}`"
    @click.self="emit('fechar')"
  >
    <header class="barra">
      <span class="posicao">{{ indice + 1 }} de {{ imagens.length }}</span>
      <div class="acoes">
        <template v-if="ehDoUsuario && !editando">
          <button type="button" class="acao" title="Editar legenda" aria-label="Editar legenda" :disabled="ocupado" @click="iniciarEdicao">
            <PencilLine :size="18" :stroke-width="1.8" aria-hidden="true" />
          </button>
          <button type="button" class="acao" title="Excluir imagem" aria-label="Excluir imagem" :disabled="ocupado" @click="excluirImagem">
            <Trash2 :size="18" :stroke-width="1.8" aria-hidden="true" />
          </button>
        </template>
        <button ref="botaoFechar" type="button" class="acao" title="Fechar (Esc)" aria-label="Fechar" @click="emit('fechar')">
          <X :size="20" :stroke-width="1.8" aria-hidden="true" />
        </button>
      </div>
    </header>

    <div class="palco" @click.self="emit('fechar')">
      <button
        type="button"
        class="navegar anterior"
        aria-label="Imagem anterior"
        :disabled="indice === 0"
        @click="irPara(indice - 1)"
      >
        <ChevronLeft :size="28" :stroke-width="1.8" aria-hidden="true" />
      </button>

      <ImagemDaAnotacao :key="imagem.id" class="imagem" :imagem="{ url: imagem.url, nome: imagem.legenda || imagem.nome }" />

      <button
        type="button"
        class="navegar seguinte"
        aria-label="Imagem seguinte"
        :disabled="indice === imagens.length - 1"
        @click="irPara(indice + 1)"
      >
        <ChevronRight :size="28" :stroke-width="1.8" aria-hidden="true" />
      </button>
    </div>

    <footer class="rodape">
      <form v-if="editando" class="edicao-legenda" @submit.prevent="salvarLegenda">
        <input
          ref="campoLegenda"
          v-model="legendaEditada"
          type="text"
          aria-label="Legenda da imagem"
          placeholder="Legenda (opcional)"
          :maxlength="TAMANHO_MAXIMO_LEGENDA"
          :disabled="ocupado"
          @keydown.esc.prevent="editando = false"
        />
        <button type="button" class="botao-cancelar" :disabled="ocupado" @click="editando = false">Cancelar</button>
        <button type="submit" class="botao-salvar" :disabled="ocupado">{{ ocupado ? 'Salvando...' : 'Salvar' }}</button>
      </form>
      <p v-else-if="imagem.legenda" class="legenda">{{ imagem.legenda }}</p>
      <p class="autor">Enviada por {{ ehDoUsuario ? 'você' : imagem.autorNome }}</p>
      <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>
    </footer>
  </div>
</template>

<style scoped>
.visualizador {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  background: rgb(15 23 42 / 92%);
  color: #f8fafc;
}

.barra {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
}

.posicao {
  color: #cbd5e1;
  font-size: 13px;
}

.acoes {
  display: flex;
  gap: 6px;
}

.acao {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: rgb(255 255 255 / 8%);
  color: #f8fafc;
  cursor: pointer;
}

.acao:hover {
  background: rgb(255 255 255 / 18%);
}

.palco {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) 56px;
  align-items: center;
  min-height: 0;
  padding: 0 8px;
}

.palco :deep(img) {
  justify-self: center;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 6px;
}

.palco :deep(.imagem-carregando),
.palco :deep(.imagem-indisponivel) {
  justify-self: center;
  width: min(480px, 100%);
}

.navegar {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: rgb(255 255 255 / 10%);
  color: #f8fafc;
  cursor: pointer;
}

.navegar:hover:not(:disabled) {
  background: rgb(255 255 255 / 22%);
}

.navegar:disabled {
  opacity: 0.25;
  cursor: default;
}

.rodape {
  display: grid;
  justify-items: center;
  gap: 4px;
  padding: 12px 16px 18px;
  text-align: center;
}

.legenda {
  max-width: 900px;
  margin: 0;
  font-size: 15px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.autor {
  margin: 0;
  color: #94a3b8;
  font-size: 12px;
}

.edicao-legenda {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  width: min(720px, 100%);
}

.edicao-legenda input {
  flex: 1 1 280px;
  min-width: 0;
  height: 36px;
  padding: 0 10px;
  border: 1px solid #475569;
  border-radius: 7px;
  background: #0f172a;
  color: #f8fafc;
  font: inherit;
  font-size: 14px;
}

.botao-cancelar,
.botao-salvar {
  min-height: 36px;
  padding: 0 14px;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.botao-cancelar {
  border: 1px solid #475569;
  background: transparent;
  color: #e2e8f0;
}

.botao-salvar {
  border: 0;
  background: #2563eb;
  color: #fff;
}

.mensagem-erro {
  margin: 0;
  color: #fca5a5;
  font-size: 13px;
}

.acao:disabled,
.botao-cancelar:disabled,
.botao-salvar:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.acao:focus-visible,
.navegar:focus-visible,
.botao-cancelar:focus-visible,
.botao-salvar:focus-visible {
  outline: 2px solid #93c5fd;
  outline-offset: 2px;
}

@media (max-width: 600px) {
  .palco {
    grid-template-columns: 40px minmax(0, 1fr) 40px;
    padding: 0;
  }

  .navegar {
    width: 36px;
    height: 36px;
  }
}
</style>
