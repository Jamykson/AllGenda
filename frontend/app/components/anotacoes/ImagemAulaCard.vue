<script setup lang="ts">
import { PencilLine, Trash2 } from '@lucide/vue'
import type { ImagemAula } from '~/types/anotacao'

// Card de uma imagem avulsa da aula. O autor edita a legenda e exclui a imagem; os demais só a veem.

const props = defineProps<{
  imagem: ImagemAula
}>()

const emit = defineEmits<{
  // a legenda foi editada ou a imagem foi excluída
  alterado: []
}>()

const { editarLegenda, removerImagemAula } = useAnotacoes()
const { usuario } = useAuth()

const editando = ref(false)
const legendaEditada = ref('')
const ocupado = ref(false)
const erro = ref('')
const campoLegenda = ref<HTMLInputElement | null>(null)

// editar e excluir são só do autor; a API responde 403 aos demais
const ehDoUsuario = computed(() => Boolean(usuario.value) && props.imagem.autorId === usuario.value?.id)

async function iniciarEdicao() {
  legendaEditada.value = props.imagem.legenda ?? ''
  erro.value = ''
  editando.value = true

  await nextTick()
  campoLegenda.value?.focus()
}

function cancelarEdicao() {
  editando.value = false
  erro.value = ''
}

async function salvarLegenda() {
  if (ocupado.value) return

  const legenda = legendaEditada.value.trim()
  if (legenda === (props.imagem.legenda ?? '')) {
    cancelarEdicao()
    return
  }

  ocupado.value = true
  erro.value = ''

  try {
    await editarLegenda(props.imagem, legenda)
    editando.value = false
    emit('alterado')
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
  if (ocupado.value || !window.confirm('Deseja excluir esta imagem?')) return

  ocupado.value = true
  erro.value = ''

  try {
    await removerImagemAula(props.imagem)
    emit('alterado')
  }
  catch (falha) {
    console.error('[anotacao] erro ao excluir imagem avulsa:', falha)
    erro.value = mensagemDeErro(falha, 'Não foi possível excluir a imagem.')
  }
  finally {
    ocupado.value = false
  }
}
</script>

<template>
  <figure class="imagem-card">
    <div v-if="ehDoUsuario && !editando" class="acoes-imagem">
      <button
        type="button"
        class="acao-imagem"
        aria-label="Editar legenda"
        title="Editar legenda"
        :disabled="ocupado"
        @click="iniciarEdicao"
      >
        <PencilLine :size="15" :stroke-width="1.8" aria-hidden="true" />
      </button>
      <button
        type="button"
        class="acao-imagem acao-excluir"
        aria-label="Excluir imagem"
        title="Excluir imagem"
        :disabled="ocupado"
        @click="excluirImagem"
      >
        <Trash2 :size="15" :stroke-width="1.8" aria-hidden="true" />
      </button>
    </div>

    <ImagemDaAnotacao :imagem="imagem" />

    <form v-if="editando" class="edicao-legenda" @submit.prevent="salvarLegenda">
      <input
        ref="campoLegenda"
        v-model="legendaEditada"
        type="text"
        aria-label="Legenda da imagem"
        placeholder="Legenda (opcional)"
        :maxlength="TAMANHO_MAXIMO_LEGENDA"
        :disabled="ocupado"
        @keydown.esc.prevent="cancelarEdicao"
      />
      <button type="button" class="btn-cancelar" :disabled="ocupado" @click="cancelarEdicao">Cancelar</button>
      <button type="submit" class="btn-salvar" :disabled="ocupado">{{ ocupado ? 'Salvando...' : 'Salvar' }}</button>
    </form>
    <figcaption v-else-if="imagem.legenda" class="legenda">{{ imagem.legenda }}</figcaption>

    <p v-if="!ehDoUsuario" class="autor-imagem">por {{ imagem.autorNome }}</p>
    <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>
  </figure>
</template>

<style scoped>
.imagem-card {
  position: relative;
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.imagem-card :deep(img) {
  width: 100%;
  max-height: 320px;
  object-fit: contain;
  border-radius: 8px;
  background: #e2e8f0;
}

.acoes-imagem {
  display: flex;
  position: absolute;
  top: 18px;
  right: 18px;
  z-index: 1;
  gap: 4px;
  padding: 2px;
  border-radius: 8px;
  background: rgb(255 255 255 / 88%);
}

.acao-imagem {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  place-items: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
}

.acao-imagem:hover {
  border-color: #e5e7eb;
  background: #f1f5f9;
  color: #2563eb;
}

.acao-excluir:hover {
  border-color: #fee2e2;
  background: #fef2f2;
  color: #b91c1c;
}

.acao-imagem:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.legenda {
  margin: 0;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.edicao-legenda {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.edicao-legenda input {
  flex: 1 1 220px;
  min-width: 0;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  border-radius: 7px;
  background: #fff;
  color: #0f172a;
  font: inherit;
  font-size: 13px;
}

.edicao-legenda input:focus {
  outline: 2px solid #bfdbfe;
  outline-offset: 1px;
}

.btn-cancelar,
.btn-salvar {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-cancelar {
  border: 1px solid #dfe3e8;
  background: #fff;
  color: #344054;
}

.btn-salvar {
  border: 0;
  background: #2563eb;
  color: #fff;
}

.btn-salvar:hover {
  background: #1d4ed8;
}

.btn-cancelar:disabled,
.btn-salvar:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.autor-imagem {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}

.mensagem-erro {
  margin: 0;
  color: #b91c1c;
  font-size: 13px;
}

.acao-imagem:focus-visible,
.btn-cancelar:focus-visible,
.btn-salvar:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}
</style>
