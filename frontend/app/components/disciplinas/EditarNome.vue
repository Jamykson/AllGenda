<script setup lang="ts">
import type { Disciplina } from '~/types/disciplina'
import { PencilLine } from '@lucide/vue'

const props = defineProps<{
  disciplina: Disciplina
}>()

const { atualizarNome } = useDisciplinas()

const editando = ref(false)
const nome = ref(props.disciplina.nome)

function salvar() {
  const novoNome = nome.value.trim()

  if (!novoNome) return

  atualizarNome(
    props.disciplina.id,
    novoNome
  )

  props.disciplina.nome = novoNome

  editando.value = false
}
</script>

<template>
  <div>
    <button
      v-if="!editando"
      type="button"
      class="botao-personalizacao"
      aria-label="Editar nome da disciplina"
      title="Editar nome"
      @click="editando = true"
    >
      <PencilLine :size="14" :stroke-width="1.8" aria-hidden="true" />
    </button>

    <div
      v-else
      class="edicao-nome"
    >
      <input
        v-model="nome"
        type="text"
        class="min-w-0 flex-1 border rounded px-3 py-2"
        @keyup.enter="salvar"
      />

      <button
        type="button"
        class="botao-salvar"
        @click="salvar"
      >
        Salvar
      </button>
    </div>
  </div>
</template>

<style scoped>
.botao-personalizacao {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  padding: 0;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
}

.botao-personalizacao:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.edicao-nome {
  display: flex;
  align-items: center;
  gap: 6px;
}

.edicao-nome input {
  width: min(220px, 55vw);
  min-width: 0;
  height: 32px;
  padding: 0 8px;
  border: 1px solid #cbd5e1;
  border-radius: 5px;
}

.edicao-nome button {
  min-height: 32px;
  padding: 0 8px;
  border: 1px solid #cbd5e1;
  border-radius: 5px;
  background: #fff;
  color: #334155;
  cursor: pointer;
}

.edicao-nome .botao-salvar {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
}
</style>