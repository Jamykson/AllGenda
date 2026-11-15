<script setup lang="ts">
import type { Participante } from '~/types/disciplina'

const props = defineProps<{
  disciplinaId: string
  participantes: Participante[]
  ehDono: boolean
  erro?: string
}>()

const emit = defineEmits<{
  removido: [usuarioId: string]
}>()

const { removerParticipante } = useDisciplinas()

const removendoId = ref<string | null>(null)
const erroRemocao = ref('')

// o dono é avisado de quantas anotações do grupo da pessoa continuam na disciplina e pode desistir
async function remover(participante: Participante) {
  const anotacoes = participante.anotacoes
  const aviso = anotacoes > 0
    ? `${participante.nome} tem ${anotacoes} ${anotacoes === 1 ? 'anotação' : 'anotações'} no grupo. `
      + 'Elas continuam na disciplina, com o nome do autor. '
    : ''
  if (!window.confirm(`Remover ${participante.nome} da disciplina? ${aviso}A pessoa perde o acesso e pode ser convidada de novo.`)) {
    return
  }
  removendoId.value = participante.usuarioId
  erroRemocao.value = ''
  try {
    await removerParticipante(props.disciplinaId, participante.usuarioId)
    emit('removido', participante.usuarioId)
  }
  catch (err) {
    console.error('[participantes] falha ao remover:', err)
    erroRemocao.value = mensagemDeErro(err, 'Não foi possível remover o participante.')
  }
  finally {
    removendoId.value = null
  }
}
</script>

<template>
  <section class="participantes" aria-label="Participantes da disciplina">
    <h3>Participantes</h3>
    <p v-if="erro || erroRemocao" class="erro" role="alert">{{ erro || erroRemocao }}</p>
    <ul class="itens">
      <li v-for="participante in participantes" :key="participante.usuarioId" class="item">
        <span class="nome">{{ participante.nome }}</span>
        <span v-if="participante.papel === 'DONO'" class="selo">Dono</span>
        <button
          v-if="ehDono && participante.papel !== 'DONO'"
          type="button"
          class="link-remover"
          :disabled="removendoId === participante.usuarioId"
          @click="remover(participante)"
        >
          Remover
        </button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.participantes {
  display: grid;
  gap: 8px;
  margin-top: 16px;
}

h3 {
  margin: 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.itens {
  display: grid;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.nome {
  overflow-wrap: anywhere;
}

.selo {
  padding: 1px 8px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  font-weight: 600;
}

.link-remover {
  margin-left: auto;
  padding: 0;
  border: 0;
  background: transparent;
  color: #b91c1c;
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.link-remover:hover:not(:disabled) {
  text-decoration: underline;
}

.erro {
  margin: 0;
  color: #b91c1c;
  font-size: 13px;
}
</style>
