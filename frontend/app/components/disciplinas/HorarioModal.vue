<script setup lang="ts">
import type { Disciplina, FrequenciaRecorrencia, HorarioRecorrente } from '~/types/disciplina'

const props = defineProps<{
  disciplina: Disciplina
  // só o dono define a recorrência; os outros participantes só a veem
  podeEditar?: boolean
}>()

const { recorrencias, carregar, salvar: salvarNoServidor, remover: removerNoServidor } = useRecorrencias()

const diasSemana = [
  { numero: 0, nome: 'Dom' },
  { numero: 1, nome: 'Seg' },
  { numero: 2, nome: 'Ter' },
  { numero: 3, nome: 'Qua' },
  { numero: 4, nome: 'Qui' },
  { numero: 5, nome: 'Sex' },
  { numero: 6, nome: 'Sáb' }
]

const editando = ref(false)
const salvando = ref(false)

function horarioVazio(): HorarioRecorrente {
  return {
    dias: [],
    horaInicio: '',
    horaFim: '',
    dataInicio: '',
    dataFim: '',
    frequencia: 'SEMANAL'
  }
}

// o que está gravado no servidor; o formulário edita uma cópia
const horarioSalvo = computed(() => recorrencias.value[props.disciplina.id] ?? null)
const horario = ref<HorarioRecorrente>(horarioVazio())

const erro = ref('')
const sucesso = ref('')

function copiaDoSalvo() {
  const salvo = horarioSalvo.value
  horario.value = salvo
    ? { ...salvo, dias: [...salvo.dias], frequencia: salvo.frequencia ?? 'SEMANAL' }
    : horarioVazio()
}

function comecarEdicao() {
  copiaDoSalvo()
  erro.value = ''
  sucesso.value = ''
  editando.value = true
}

function cancelarEdicao() {
  editando.value = false
  erro.value = ''
}

function alternarDia(dia: number) {
  if (horario.value.dias.includes(dia)) {
    horario.value.dias = horario.value.dias.filter(item => item !== dia)
  }
  else {
    horario.value.dias.push(dia)
  }
}

async function salvar() {
  erro.value = ''
  sucesso.value = ''

  if (horario.value.dias.length === 0) {
    erro.value = 'Selecione pelo menos um dia da semana.'
    return
  }

  if (!horario.value.horaInicio || !horario.value.horaFim) {
    erro.value = 'Informe o horário de início e fim.'
    return
  }

  if (!horario.value.dataInicio || !horario.value.dataFim) {
    erro.value = 'Informe a data inicial e final.'
    return
  }

  if (horario.value.horaFim <= horario.value.horaInicio) {
    erro.value = 'O horário final deve ser depois do horário inicial.'
    return
  }

  if (horario.value.dataFim < horario.value.dataInicio) {
    erro.value = 'A data final deve ser depois da data inicial.'
    return
  }

  salvando.value = true
  try {
    await salvarNoServidor(props.disciplina.id, {
      ...horario.value,
      dias: [...horario.value.dias]
    })
    sucesso.value = 'Horário salvo com sucesso.'
    editando.value = false
  }
  catch (err) {
    console.error('[recorrencia] falha ao salvar:', err)
    erro.value = mensagemDeErro(err, 'Não foi possível salvar o horário.')
  }
  finally {
    salvando.value = false
  }
}

async function remover() {
  if (!window.confirm('Remover o horário recorrente? As aulas que já têm anotações continuam na disciplina.')) {
    return
  }
  erro.value = ''
  sucesso.value = ''
  try {
    await removerNoServidor(props.disciplina.id)
    editando.value = false
  }
  catch (err) {
    console.error('[recorrencia] falha ao remover:', err)
    erro.value = mensagemDeErro(err, 'Não foi possível remover o horário.')
  }
}

const rotuloFrequencia = (frequencia?: FrequenciaRecorrencia) =>
  frequencia === 'QUINZENAL' ? 'Quinzenal' : 'Semanal'

const possuiHorario = computed(() => {
  const salvo = horarioSalvo.value
  return Boolean(salvo && salvo.dias.length > 0 && salvo.horaInicio && salvo.horaFim)
})

const diasSelecionados = computed(() => {
  const salvo = horarioSalvo.value
  return diasSemana
    .filter(dia => salvo?.dias.includes(dia.numero))
    .map(dia => dia.nome)
    .join(', ')
})

onMounted(() => carregar())
</script>


<template>
  <div class="horarios">
    <div class="cabecalho">
      <div>
        <h3>Horários recorrentes</h3>

        <p>
          Configure quando esta disciplina acontece.
        </p>
      </div>

    </div>


    <div
      v-if="possuiHorario && !editando"
      class="horario-salvo"
    >
      <div>
        <strong>
          {{ diasSelecionados }} · {{ rotuloFrequencia(horarioSalvo?.frequencia) }}
        </strong>

        <p>
          {{ horarioSalvo?.horaInicio }}
          –
          {{ horarioSalvo?.horaFim }}
        </p>

        <small>
          {{ horarioSalvo?.dataInicio }}
          até
          {{ horarioSalvo?.dataFim }}
        </small>
      </div>

      <div v-if="podeEditar" class="acoes">
        <button
          type="button"
          @click="comecarEdicao"
        >
          Editar
        </button>

        <button
          type="button"
          class="remover"
          @click="remover"
        >
          Remover
        </button>
      </div>
    </div>


    <div
      v-else-if="!editando"
      class="sem-horario"
    >
      Nenhum horário configurado.
      <button v-if="podeEditar" type="button" class="definir" @click="comecarEdicao">
        Definir horário
      </button>
    </div>


    <div
      v-if="editando"
      class="formulario"
    >
      <label>
        Dias da semana
      </label>

      <div class="dias">
        <button
          v-for="dia in diasSemana"
          :key="dia.numero"
          type="button"
          class="dia"
          :class="{
            ativo:
              horario.dias.includes(
                dia.numero
              )
          }"
          @click="
            alternarDia(dia.numero)
          "
        >
          {{ dia.nome }}
        </button>
      </div>


      <div class="linha">
        <div>
          <label>
            Frequência
          </label>

          <select v-model="horario.frequencia">
            <option value="SEMANAL">Toda semana</option>
            <option value="QUINZENAL">A cada duas semanas</option>
          </select>
        </div>
      </div>


      <div class="linha">
        <div>
          <label>
            Início
          </label>

          <input
            v-model="horario.horaInicio"
            type="time"
          />
        </div>

        <div>
          <label>
            Fim
          </label>

          <input
            v-model="horario.horaFim"
            type="time"
          />
        </div>
      </div>


      <div class="linha">
        <div>
          <label>
            Data inicial
          </label>

          <input
            v-model="horario.dataInicio"
            type="date"
          />
        </div>

        <div>
          <label>
            Data final
          </label>

          <input
            v-model="horario.dataFim"
            type="date"
          />
        </div>
      </div>


      <p
        v-if="erro"
        class="erro"
      >
        {{ erro }}
      </p>


      <div class="botoes">
        <button
          type="button"
          class="cancelar"
          :disabled="salvando"
          @click="cancelarEdicao"
        >
          Cancelar
        </button>

        <button
          type="button"
          class="salvar"
          :disabled="salvando"
          @click="salvar"
        >
          {{ salvando ? 'Salvando...' : 'Salvar horário' }}
        </button>
      </div>
    </div>


    <p
      v-if="sucesso"
      class="sucesso"
    >
      {{ sucesso }}
    </p>
  </div>
</template>


<style scoped>
.horarios {
  margin-top: 20px;
}

.cabecalho {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.cabecalho h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.cabecalho p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}

.sem-horario {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 14px;
}

.horario-salvo {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.horario-salvo p {
  margin: 4px 0;
  color: #475569;
}

.horario-salvo small {
  color: #94a3b8;
}

.acoes {
  display: flex;
  gap: 8px;
}

.acoes button {
  min-height: 30px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 7px;
  padding: 7px 10px;
  cursor: pointer;
}

.acoes .remover {
  color: #dc2626;
}

.formulario {
  margin-top: 18px;
}

.formulario label {
  display: block;
  margin-bottom: 7px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.dias {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: 18px;
}

.dia {
  min-height: 32px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: white;
  padding: 0 10px;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.dia.ativo {
  border-color: #2563eb;
  background: #eff6ff;
  color: #2563eb;
}

.linha {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 15px;
}

select,
input {
  width: 100%;
  min-height: 36px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 7px 9px;
  outline: none;
}

select:focus,
input:focus {
  border-color: #2563eb;
}

.definir {
  margin-left: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #2563eb;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.botoes {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}

.cancelar,
.salvar {
  min-height: 34px;
  border-radius: 6px;
  padding: 0 12px;
  cursor: pointer;
  font-weight: 600;
}

.cancelar {
  border: 1px solid #cbd5e1;
  background: white;
  color: #334155;
}

.salvar {
  border: 1px solid #2563eb;
  background: #2563eb;
  color: white;
}

.cancelar:hover {
  background: #f8fafc;
}

.salvar:hover {
  background: #1d4ed8;
}

.dia:focus-visible,
.acoes button:focus-visible,
.cancelar:focus-visible,
.salvar:focus-visible,
input:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}

.erro {
  color: #dc2626;
  font-size: 13px;
}

.sucesso {
  margin-top: 10px;
  color: #16a34a;
  font-size: 13px;
}
</style>