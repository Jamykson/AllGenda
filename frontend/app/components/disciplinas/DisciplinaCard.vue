<script setup lang="ts">
import type { Disciplina } from '~/types/disciplina'
import { PencilLine, Trash2 } from '@lucide/vue'

const props = defineProps<{
  disciplina: Disciplina
}>()

const emit = defineEmits<{
  abrir: []
  editar: []
  excluir: []
}>()

interface HorarioRecorrente {
  dias: number[]
  horaInicio: string
  horaFim: string
  dataInicio: string
  dataFim: string
}

const CHAVE_HORARIOS = 'allgenda-horarios-recorrentes'
const nomesDias = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SAB']
const horario = ref<HorarioRecorrente | null>(null)

const diasHorario = computed(() => {
  const diasSalvos = horario.value?.dias || []
  return nomesDias.filter((_, indice) => diasSalvos.includes(indice))
})

const horarioCompleto = computed(() =>
  Boolean(horario.value?.horaInicio && horario.value.horaFim)
)

function carregarHorario() {
  if (import.meta.server) return

  try {
    const dados = JSON.parse(localStorage.getItem(CHAVE_HORARIOS) || '{}') as Record<string, HorarioRecorrente>
    const salvo = dados[props.disciplina.id]
    horario.value = salvo && Array.isArray(salvo.dias) ? salvo : null
  }
  catch {
    horario.value = null
  }
}

onMounted(carregarHorario)
watch(() => props.disciplina.id, carregarHorario)
</script>

<template>
  <article
    class="disciplina-card"
    :style="{ '--cor-disciplina': disciplina.cor || '#94a3b8' }"
  >
    <div
      class="conteudo-card"
      role="button"
      tabindex="0"
      :aria-label="`Abrir aulas de ${disciplina.nome}`"
      @click="emit('abrir')"
      @keydown.enter.prevent="emit('abrir')"
      @keydown.space.prevent="emit('abrir')"
    >
      <div class="identidade-card">
        <span class="indicador-cor" aria-hidden="true" />
        <span class="icone-disciplina" aria-hidden="true">
          {{ disciplina.icone || disciplina.nome[0] }}
        </span>
        <h3>{{ disciplina.nome }}</h3>
      </div>

      <p v-if="disciplina.descricao" class="descricao-disciplina">
        {{ disciplina.descricao }}
      </p>

      <div v-if="diasHorario.length || horarioCompleto" class="dados-horario">
        <span v-if="diasHorario.length" class="dias-horario">
          {{ diasHorario.join(' · ') }}
        </span>
        <span v-if="horarioCompleto" class="horas-horario">
          {{ horario?.horaInicio }} – {{ horario?.horaFim }}
        </span>
      </div>
    </div>

    <div class="acoes-card" aria-label="Ações da disciplina">
      <button
        type="button"
        class="botao-acao"
        :aria-label="`Editar ${disciplina.nome}`"
        title="Editar disciplina"
        @click="emit('editar')"
      >
        <PencilLine :size="15" :stroke-width="1.8" aria-hidden="true" />
      </button>
      <button
        type="button"
        class="botao-acao botao-excluir"
        :aria-label="`Excluir ${disciplina.nome}`"
        title="Excluir disciplina"
        @click="emit('excluir')"
      >
        <Trash2 :size="15" :stroke-width="1.8" aria-hidden="true" />
      </button>
    </div>
  </article>
</template>

<style scoped>
.disciplina-card {
  display: flex;
  min-height: 92px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  transition: border-color 150ms ease, background-color 150ms ease;
}

.disciplina-card:hover {
  border-color: #cbd5e1;
  background: #fbfdff;
}

.conteudo-card {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  align-items: stretch;
  gap: 7px;
  cursor: pointer;
}

.conteudo-card:focus-visible {
  border-radius: 6px;
  outline: 2px solid currentColor;
  outline-offset: 4px;
}

.identidade-card {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.indicador-cor {
  width: 9px;
  height: 9px;
  flex: 0 0 9px;
  border-radius: 50%;
  background: var(--cor-disciplina);
}

.icone-disciplina {
  display: grid;
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  place-items: center;
  border-radius: 6px;
  background: #f1f5f9;
  font-size: 16px;
}

.identidade-card h3 {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #1f2937;
  font-size: 14px;
  font-weight: 650;
  line-height: 1.3;
}

.descricao-disciplina {
  padding-left: 45px;
  color: #64748b;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.dados-horario {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 10px;
  padding-left: 45px;
  color: #64748b;
  font-size: 11px;
  line-height: 1.4;
}

.dias-horario {
  color: #475569;
  font-weight: 650;
}

.acoes-card {
  display: flex;
  flex: 0 0 auto;
  gap: 4px;
}

.botao-acao {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
}

.botao-acao:hover {
  border-color: #e5e7eb;
  background: #f8fafc;
  color: #334155;
}

.botao-excluir:hover {
  color: #b91c1c;
}

.botao-acao:focus-visible {
  outline: 2px solid currentColor;
  outline-offset: 2px;
}

@media (hover: none) {
  .acoes-card { opacity: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .disciplina-card {
    transition: none;
  }
}
</style>