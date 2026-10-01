<script setup lang="ts">
import type { Disciplina } from '~/types/disciplina'

const props = defineProps<{
  disciplina: Disciplina
}>()

interface HorarioRecorrente {
  dias: number[]
  horaInicio: string
  horaFim: string
  dataInicio: string
  dataFim: string
}

const CHAVE_HORARIOS =
  'allgenda-horarios-recorrentes'

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

const horario = ref<HorarioRecorrente>({
  dias: [],
  horaInicio: '',
  horaFim: '',
  dataInicio: '',
  dataFim: ''
})

const erro = ref('')
const sucesso = ref('')


function carregar() {
  if (import.meta.server) return

  const salvo =
    localStorage.getItem(CHAVE_HORARIOS)

  if (!salvo) return

  try {
    const horarios =
      JSON.parse(salvo)

    const horarioSalvo =
      horarios[props.disciplina.id]

    if (horarioSalvo) {
      horario.value = {
        dias: horarioSalvo.dias || [],
        horaInicio: horarioSalvo.horaInicio || '',
        horaFim: horarioSalvo.horaFim || '',
        dataInicio: horarioSalvo.dataInicio || '',
        dataFim: horarioSalvo.dataFim || ''
      }
    }
  }
  catch (e) {
    console.error(
      'Erro ao carregar horários:',
      e
    )
  }
}


function alternarDia(dia: number) {
  if (
    horario.value.dias.includes(dia)
  ) {
    horario.value.dias =
      horario.value.dias.filter(
        item => item !== dia
      )
  }
  else {
    horario.value.dias.push(dia)
  }
}


function salvar() {
  erro.value = ''
  sucesso.value = ''

  if (
    horario.value.dias.length === 0
  ) {
    erro.value =
      'Selecione pelo menos um dia da semana.'
    return
  }

  if (
    !horario.value.horaInicio ||
    !horario.value.horaFim
  ) {
    erro.value =
      'Informe o horário de início e fim.'
    return
  }

  if (
    !horario.value.dataInicio ||
    !horario.value.dataFim
  ) {
    erro.value =
      'Informe a data inicial e final.'
    return
  }

  if (
    horario.value.horaFim <=
    horario.value.horaInicio
  ) {
    erro.value =
      'O horário final deve ser depois do horário inicial.'
    return
  }

  if (
    horario.value.dataFim <
    horario.value.dataInicio
  ) {
    erro.value =
      'A data final deve ser depois da data inicial.'
    return
  }

  let horarios: Record<
    string,
    HorarioRecorrente
  > = {}

  const salvo =
    localStorage.getItem(CHAVE_HORARIOS)

  if (salvo) {
    try {
      horarios =
        JSON.parse(salvo)
    }
    catch {
      horarios = {}
    }
  }

  horarios[props.disciplina.id] = {
    ...horario.value,
    dias: [...horario.value.dias]
  }

  localStorage.setItem(
    CHAVE_HORARIOS,
    JSON.stringify(horarios)
  )

  sucesso.value =
    'Horário salvo com sucesso.'

  editando.value = false
}


function remover() {
  const salvo =
    localStorage.getItem(CHAVE_HORARIOS)

  if (salvo) {
    try {
      const horarios =
        JSON.parse(salvo)

      delete horarios[
        props.disciplina.id
      ]

      localStorage.setItem(
        CHAVE_HORARIOS,
        JSON.stringify(horarios)
      )
    }
    catch {
      // ignora localStorage inválido
    }
  }

  horario.value = {
    dias: [],
    horaInicio: '',
    horaFim: '',
    dataInicio: '',
    dataFim: ''
  }

  erro.value = ''
  sucesso.value = ''
  editando.value = false
}


const possuiHorario =
  computed(() =>
    horario.value.dias.length > 0 &&
    horario.value.horaInicio &&
    horario.value.horaFim
  )


const diasSelecionados =
  computed(() => {
    return diasSemana
      .filter(dia =>
        horario.value.dias.includes(
          dia.numero
        )
      )
      .map(dia => dia.nome)
      .join(', ')
  })


onMounted(() => {
  carregar()
})
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
          {{ diasSelecionados }}
        </strong>

        <p>
          {{ horario.horaInicio }}
          –
          {{ horario.horaFim }}
        </p>

        <small>
          {{ horario.dataInicio }}
          até
          {{ horario.dataFim }}
        </small>
      </div>

      <div class="acoes">
        <button
          type="button"
          @click="editando = true"
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
          @click="editando = false"
        >
          Cancelar
        </button>

        <button
          type="button"
          class="salvar"
          @click="salvar"
        >
          Salvar horário
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
  margin-top: 24px;
}

.cabecalho {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.cabecalho h3 {
  margin: 0;
  font-size: 16px;
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
  padding: 16px;
  border-radius: 10px;
  background: #f8fafc;
  color: #64748b;
  font-size: 14px;
}

.horario-salvo {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
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
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: white;
  padding: 8px 11px;
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

input {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 9px 10px;
  outline: none;
}

input:focus {
  border-color: #2563eb;
}

.botoes {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}

.cancelar,
.salvar {
  border-radius: 8px;
  padding: 9px 14px;
  cursor: pointer;
  font-weight: 600;
}

.cancelar {
  border: 1px solid #cbd5e1;
  background: white;
}

.salvar {
  border: none;
  background: #2563eb;
  color: white;
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