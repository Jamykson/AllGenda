<script setup lang="ts">
import type { Disciplina } from '~/types/disciplina'

defineProps<{
  disciplina: Disciplina
}>()

const emit = defineEmits<{
  fechar: []
}>()
</script>

<template>
  <div class="modal-overlay" @click.self="emit('fechar')">
    <section
      class="modal-painel"
      role="dialog"
      aria-modal="true"
      aria-labelledby="titulo-aulas-disciplina"
    >
      <header class="modal-header">
        <div>
          <h2 id="titulo-aulas-disciplina">Aulas</h2>
        </div>
        <button
          type="button"
          class="botao-fechar"
          aria-label="Fechar lista de aulas"
          @click="emit('fechar')"
        >
          ×
        </button>
      </header>

      <ListaAulasDisciplina
        :disciplina-id="disciplina.id"
        :cor="disciplina.cor || '#2563eb'"
        :disciplina-nome="disciplina.nome"
      />
    </section>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  overflow-y: auto;
  padding: 16px;
  background: rgb(15 23 42 / 40%);
}

.modal-painel {
  width: min(700px, 100%);
  max-height: calc(100dvh - 32px);
  box-sizing: border-box;
  overflow-y: auto;
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 14px 40px rgb(15 23 42 / 14%);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 0;
}

.modal-header h2 {
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.modal-header p {
  margin-top: 4px;
  color: #64748b;
  font-size: 14px;
}

:deep(.lista-aulas.com-nome-disciplina) {
  margin-top: 8px;
  padding-top: 0;
  border-top: 0;
}

.botao-fechar {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #64748b;
  font-size: 22px;
  cursor: pointer;
}

.botao-fechar:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.botao-fechar:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}

@media (max-width: 520px) {
  .modal-overlay {
    align-items: flex-start;
    padding: 10px;
  }

  .modal-painel {
    max-height: calc(100dvh - 20px);
    padding: 18px;
  }
}
</style>