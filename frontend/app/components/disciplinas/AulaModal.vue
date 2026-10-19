<script setup lang="ts">
import type { AulaDaAnotacao } from '~/types/anotacao'

// Modal de uma aula: cabeçalho com a data e o horário, e as anotações dela (AnotacoesAula).

const props = defineProps<{
  aula: AulaDaAnotacao | null
}>()

const emit = defineEmits<{
  fechar: []
  // alguma anotação foi criada, editada ou excluída
  alterado: []
  // uma aula recorrente foi cadastrada no backend ao receber a primeira anotação
  'aula-cadastrada': [aula: AulaDaAnotacao]
}>()

const titulo = ref('')

watch(
  () => props.aula,
  (aula) => {
    titulo.value = aula?.topico || 'Aula'
  },
  { immediate: true }
)

function aoCadastrarAula(aula: AulaDaAnotacao) {
  titulo.value = aula.topico || titulo.value
  emit('aula-cadastrada', aula)
}

function formatarData(data: string) {
  const [ano, mes, dia] = data.split('-').map(Number)
  if (!ano || !mes || !dia) return data

  return new Intl.DateTimeFormat('pt-BR', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  }).format(new Date(ano, mes - 1, dia))
}
</script>

<template>
  <div v-if="aula" class="modal-overlay" @click.self="emit('fechar')">
    <section class="modal-aula" role="dialog" aria-modal="true" aria-labelledby="titulo-aula-modal">
      <header class="cabecalho-modal">
        <div>
          <h2 id="titulo-aula-modal">{{ titulo }}</h2>
          <p>{{ formatarData(aula.data) }} · {{ aula.horario }}</p>
        </div>
        <button type="button" class="botao-fechar" aria-label="Fechar aula" @click="emit('fechar')">
          ×
        </button>
      </header>

      <AnotacoesAula
        :aula="aula"
        @alterado="emit('alterado')"
        @aula-cadastrada="aoCadastrarAula"
      />
    </section>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(15 23 42 / 35%);
  padding: 20px;
}

.modal-aula {
  width: min(760px, 100%);
  max-height: min(88vh, 760px);
  overflow-y: auto;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 18px 50px rgb(15 23 42 / 16%);
  padding: 20px 22px 18px;
}

.cabecalho-modal {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 18px;
}

.cabecalho-modal h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 700;
}

.cabecalho-modal p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.botao-fechar {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
}

.botao-fechar:hover {
  background: #f1f5f9;
  color: #0f172a;
}

@media (max-width: 560px) {
  .modal-aula {
    padding: 16px 14px;
  }

  .cabecalho-modal {
    align-items: center;
  }
}
</style>
